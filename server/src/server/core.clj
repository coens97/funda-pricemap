(ns server.core
  (:require [flambo.conf :as conf]
            [flambo.api :as f]
            [server
             [request :refer [get-token nr-of-pages house-ids]]]))

(defn -main
  "Entry point"
  []
  (def c (-> (conf/spark-conf)
             (conf/master "local[*]")
             (conf/app-name "funda-crawler")))

  (def sc (f/spark-context c))
  (let [token (get-token)]
    (println
    ;; Distribute work
     (-> (f/parallelize sc (range 1 (nr-of-pages token)))
          ;; Go through the list of houses available
         (f/flat-map (f/iterator-fn [page] (house-ids token page)))
         (f/map (f/fn [x] (* x x)))
         f/collect)))

  (Thread/sleep (* 1000 60 60 24)))