(defproject server "0.1.0-SNAPSHOT"
  :description "Funda crawler server"
  :url "https://github.com/coens97/funda-pricemap"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [http-kit "2.2.0"]
                 [org.clojure/data.json "0.2.6"]
                 [yieldbot/flambo "0.8.2"]
                 [danlentz/clj-uuid "0.1.7"]]
  :aot [server.TopologySubmitter]
  :main server.core
  ;; include storm dependency only in dev because production storm cluster provides it
  :profiles {:dev
             {:aot [flambo.function]
              :dependencies [[org.apache.spark/spark-core_2.11 "2.2.0"]]}})
