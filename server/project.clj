(defproject server "0.1.0-SNAPSHOT"
  :description "Funda crawler server"
  :url "https://github.com/coens97/funda-pricemap"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [http-kit "2.2.0"]
                 [danlentz/clj-uuid "0.1.7"]]
  :aot [server.TopologySubmitter]
  ;; include storm dependency only in dev because production storm cluster provides it
  :profiles {:dev {:dependencies [[storm "0.8.1"]]}})
