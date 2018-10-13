(ns server.core
  (:require [flambo.conf :as conf]
            [flambo.api :as f]
            [flambo.tuple :as ft]
            [clojure.data.json :as json]
            [server
             [request :refer [get-token nr-of-pages house-ids house-details]]]))

(defn -main
  "Entry point"
  []
  (def c (-> (conf/spark-conf)
             (conf/master "local[*]")
             (conf/app-name "funda-crawler")))

  (def sc (f/spark-context c))
  (let [token (get-token)
        result (-> (f/parallelize sc (range 1  3));;(nr-of-pages token)))
                    ;; Go through the list of houses available
                   (f/flat-map (f/iterator-fn [page] (house-ids token page)))
                    ;; Retrieve each house
                   (f/map (f/fn [x] (house-details token x)))
                    ;; Remove houses that are not included
                   (f/filter (f/fn [x] (some? x)))
                    ;; Group by postal code
                   (f/group-by (f/fn [{postcode :postcode}] postcode))
                    ;; Change from spark tuple to clojure haspmap
                   (f/map (ft/key-val-fn (f/fn [k v] {k v})))
                    ;; Collect result from spark
                   f/collect)
        jsontxt (->> result
                      ;; Combine hashmaps
                     (apply merge)
                      ;; To JSON
                     json/write-str)]
    (spit "docs/generated/data.json" jsontxt))

  (Thread/sleep (* 1000 60 60 24)))