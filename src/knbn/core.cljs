(ns knbn.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [cljs.core.async :as async]

            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]))

(enable-console-print!)

(def colors {:done "#8ae234"
             :open "#ad7fa8"
             :wip "#fcaf3e"})

(def app-state (atom {:text "Personal Kanban"
                      :max-wip-tasks 3
                      :tasks [{:text "Wäsche waschen"
                               :state :open}
                              {:text "Wohnung auf Vordermann bringen"
                               :state :wip}
                              {:text "Fussball schauen"
                               :state :done}
                              {:text "Einladungen verschicken"
                               :state :done}
                              ]}))

(defn wip-limit-exceeded? [{:keys [tasks max-wip-tasks]}]
  (-> (filter #(= :wip (:state %)) tasks)
       count
       (>= max-wip-tasks)))

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
                           (swap! app-state update-in [:tasks] conj {:text text :state :open :color (:open colors)}))
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

(defcomponent task-comp [{:keys [text state]} owner]
  (render [_]
          (dom/li
           (dom/div {:style {:background-color (get colors state)}
                     :class "uk-panel uk-panel-box uk-text-center task"} text))))

(defcomponent body-comp [{:keys [tasks] :as app} owner]
  (render-state [_ state]
                (let [{:keys [done wip open]} (group-by :state tasks)]
                  (dom/div {:class "uk-grid"}

                           (dom/div {:class "uk-width-1-3 uk-panel
                                     open-col"}
                                    (dom/ul {:class "uk-grid uk-grid-width-small-1-1 uk-grid-width-medium-1-2 uk-grid-width-large-1-2"}
                                            (om/build-all task-comp open)))

                           (dom/div {:class "uk-width-1-3 uk-panel
                                     wip-col"}
                                    (dom/ul {:class "uk-grid uk-grid-width-small-1-1 uk-grid-width-medium-1-2 uk-grid-width-large-1-2"}
                                            (om/build-all task-comp wip)))

                           (dom/div {:class "uk-width-1-3 uk-panel
                                     done-col"}
                                    (dom/ul {:class "uk-grid uk-grid-width-small-1-1 uk-grid-width-medium-1-2 uk-grid-width-large-1-2"}
                                            (om/build-all task-comp done)))
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
