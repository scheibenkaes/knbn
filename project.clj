(defproject org.scheibenkaes/knbn "1.0.0"
  :description "A personal kanban board using local storage"
  :url "http://github.com/scheibenkaes/knbn"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/core.match "0.2.1"]
                 [om "0.7.1"]
                 [prismatic/om-tools "0.3.2"]

                 [org.scheibenkaes/attic "0.2.0"]
                 [org.scheibenkaes/toolbelt "0.1.0"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src"]

  :cljsbuild {
              :builds [{:id "knbn"
                        :source-paths ["src"]
                        :compiler {
                                   :output-to "resources/public/knbn.js"
                                   :output-dir "resources/public/out"
                                   :optimizations :none
                                   :source-map true}}
                       {:id "prod"
                        :source-paths ["src"]
                        :compiler {
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/react.js" "externs/uikit.externs.js"]
                                   :output-to "resources/public/knbn.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :closure-warnings {:externs-validation :off
                                                      :non-standard-jsdoc :off}}}]})
