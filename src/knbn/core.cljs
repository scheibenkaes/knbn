(ns knbn.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.match.macros :refer [match]])
  (:require [om.core :as om :include-macros true]
            [cljs.core.async :as async]
            [cljs.core.match]
            [cljs.reader]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [attic.core :as attic]
            [toolbelt.core :as toolbelt]))

(enable-console-print!)

(def colors {:done "8ae234"
             :open "ad7fa8"
             :wip "fcaf3e"})

(def db "knkn-db")

(def placeholder {:state :hidden
                  :text "Nothing here right now"})

(defn notify-task-updated [msg & [status]]
  (js/$.UIkit.notify #js {:message msg
                          :status (or status "info")
                          :pos "bottom-center"
                          :timeout 1000}))

(defn load-tasks-from-local-storage []
  (let [tasks (attic/get-item db)]
    (vec tasks)))

(defn initial-state [tasks]
  {:text "Personal Kanban"
   :max-wip-tasks 5
   :tasks tasks})

(def app-state (atom (initial-state (load-tasks-from-local-storage))))

(defn auto-save [k r {old-tasks :tasks} {new-tasks :tasks}]
  (when-not (= old-tasks new-tasks)
    (attic/set-item db new-tasks)))

(add-watch app-state ::task-auto-save auto-save)

(defn update-state-in [tasks task state]
  (mapv (fn [{id :id :as t}]
          (if (= id (:id task))
            (assoc t :state state)
            t)) tasks))

(def intercom (async/chan))

(defn update-task [task new-state]
  (let [tasks (:tasks @app-state)
        former-tasks tasks
        new-tasks (update-state-in tasks task new-state)]
    (swap! app-state assoc :tasks new-tasks)
    (when (not= former-tasks new-tasks)
      (notify-task-updated (str "Updated " (:text task)) "success")))
  )

(defn delete-task [id]
  (swap! app-state update-in [:tasks] (comp vec (toolbelt/flip remove)) (fn [{id-t :id}] (= id id-t))))

(go-loop []
         (let [msg (async/<! intercom)]
           (match [msg]
                  [{:task task :new-state new-state}] (update-task task new-state)
                  [{:delete id}] (delete-task id)))

         (recur))

(defn wip-limit-exceeded? [{:keys [tasks max-wip-tasks]}]
  (-> (filter #(= :wip (:state %)) tasks)
      count
      (>= max-wip-tasks)))

(defn new-id [] (js/Date.now))

(defcomponent header-comp [app owner]
  (init-state [_]
              {:comm (async/chan)})

  (will-mount [_]
              (go-loop []
                       (let [comm (om/get-state owner :comm)
                             msg (async/<! comm)
                             input (om/get-node owner "add-task")
                             text (or
                                   (-> input .-value)
                                   "")]
                         (when (seq (.trim text))
                           (set! (.-value input) nil)
                           (swap! app-state update-in [:tasks] conj
                                  {:text text :state :open :color (:open colors) :id (new-id)}))
                         (recur))))

  (render-state [_ {:keys [comm] :as state}]
                (dom/div {:class "uk-grid"}
                         (dom/div {:class "uk-width-1-3 uk-panel"}
                                  (dom/h3 {:class "uk-panel-title"} "Open")
                                  (dom/form {:on-submit (fn [e]
                                                          (.preventDefault e)
                                                          (async/put! comm :on-submit))}
                                            (dom/input {:type "text" :placeholder "New task"
                                                        :ref "add-task"
                                                        :on-submit (fn [e] (js/console.log (.-target e)) )})))
                         (dom/div {:class "uk-width-1-3 uk-panel "}
                                  (dom/h3 {:class "uk-panel-title"} "WIP" " (" (:max-wip-tasks app) ")"))
                         (dom/div {:class "uk-width-1-3 uk-panel"}
                                  (dom/h3 {:class "uk-panel-title"} "Closed")))))

(defcomponent task-comp [{:keys [text state id] :as task} owner]
  (render-state [_ {:keys [draggable]}]
                (let [draggable (and draggable (not= state :hidden))]
                  (dom/li
                   (dom/div {:style {:background-color (get colors state)
                                     :cursor (when draggable "pointer")}
                             :class (str "uk-panel uk-panel-box uk-text-center
                                         task " (name state))
                             :draggable draggable
                             :on-drag-over (fn [e]
                                             (.preventDefault e)
                                             false)

                             :on-drag-start (fn[e]
                                              (.setData (.-dataTransfer e) "application/clojure" @task))}
                            text
                            (when (= state :done)
                              (dom/a {:class "uk-close"
                                      :on-click (fn [_] (async/put! intercom {:delete id}))}))
                            )))))

(defcomponent col-comp [tasks owner]
  (render-state [_ {:keys [draggable task-state]}]
                (dom/div {:class "uk-width-1-3 uk-panel
                          "
                          :on-drop (fn [e]
                                     (.stopPropagation e)
                                     (let [task (.getData (.-dataTransfer e) "application/clojure")]
                                       (async/put! intercom {:task (cljs.reader/read-string task)
                                                             :new-state task-state}))
                                     )}
                         (dom/ul {:class "uk-grid uk-grid-width-small-1-1 uk-grid-width-medium-1-2 uk-grid-width-large-1-2"}
                                 (om/build-all task-comp tasks {:state {:draggable draggable}})))))

(defcomponent body-comp [{:keys [tasks] :as app} owner]
  (render-state [_ state]
                (let [{:keys [done wip open]} (group-by :state tasks)]
                  (dom/div {:class "uk-grid"}

                           (om/build col-comp (or open [placeholder]) {:state {:draggable (not
                                                                                           (wip-limit-exceeded? app))
                                                                               :task-state :open}})

                           (om/build col-comp (or wip [placeholder]) {:state {:draggable true
                                                                              :task-state :wip}})

                           (om/build col-comp (or done [placeholder])
                                     {:state {:draggable false
                                              :task-state :done}})

                           ))))


(om/root
 (fn [app owner]
   (reify om/IRender
     (render [_]
             (dom/div {:class "uk-container"}
                      (om/build header-comp app)
                      (om/build body-comp app)))))
 app-state
 {:target (. js/document (getElementById "app"))})
