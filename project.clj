(defproject org.scheibenkaes/knbn "0.1.0-SNAPSHOT"
  :description "A personal kanban board using local storage"
  :url "http://github.com/scheibenkaes/knbn"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.7.1"]
                 [prismatic/om-tools "0.3.2"]

                 [org.scheibenkaes/attic "0.2.0"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "knbn"
              :source-paths ["src"]
              :compiler {
                :output-to "knbn.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
