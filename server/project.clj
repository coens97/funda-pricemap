(defproject server "0.1.0-SNAPSHOT"
  :description "Funda crawler server"
  :url "https://github.com/coens97/funda-pricemap"
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :aot [server.TopologySubmitter]
  ;; include storm dependency only in dev because production storm cluster provides it
  :profiles {:dev {:dependencies [[storm "0.8.1"]]}})
