(ns server.topology
  "Topology

More info on the Clojure DSL here:

https://github.com/nathanmarz/storm/wiki/Clojure-DSL"
  (:require [server
             [spouts :refer [token-spout]]
             [bolts :refer [stormy-bolt server-bolt houselist-bolt]]]
            [backtype.storm [clojure :refer [topology spout-spec bolt-spec]] [config :refer :all]])
  (:import [backtype.storm LocalCluster LocalDRPC]))

(defn stormy-topology []
  (topology
   {"spout" (spout-spec token-spout)}

   ;;{"stormy-bolt" (bolt-spec {"spout" ["type"]} stormy-bolt :p 2)
   ;; "server-bolt" (bolt-spec {"stormy-bolt" :shuffle} server-bolt :p 2)}))
   {"houselist-bolt" (bolt-spec {"spout" ["token"]} houselist-bolt :p 1)}))

(defn run! [& {debug "debug" workers "workers" :or {debug "true" workers "2"}}]
  (doto (LocalCluster.)
    (.submitTopology "stormy topology"
                     {TOPOLOGY-DEBUG (Boolean/parseBoolean debug)
                      TOPOLOGY-WORKERS (Integer/parseInt workers)}
                     (stormy-topology))))
