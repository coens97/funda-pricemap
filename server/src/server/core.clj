(ns server.core
  (:require [flambo.conf :as conf]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clj-time.local :as l]
            [flambo.api :as f]
            [flambo.tuple :as ft]
            [perseverance.core :as p]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [server
             [request :refer [get-token nr-of-pages house-ids house-details]]]))

(defn register-file
  ""
  [date]
  (let [readjson (->
                  (slurp "docs/generated/overview.json")
                  json/read-str)
        {dates "dates"} readjson
        updatejson (->
                    readjson
                    (assoc "dates" (conj dates date))
                    json/write-str)]
    (spit "docs/generated/overview.json" updatejson)))

(defn run-batch
  "Process the data from funda through Spark"
  [sc]
  (let [date (l/format-local-time (l/local-now) :date)
        filename (str "docs/generated/" date ".json")]
    (if (not (.exists (io/as-file filename)))
      (let [token (get-token)
            result (->
                    (f/parallelize sc (range 1  3));;(nr-of-pages token)))
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
            jsontxt (->>
                     result
                      ;; Combine hashmaps
                     (apply merge)
                      ;; To JSON
                     json/write-str)]
          ;; Write to file
        (spit filename jsontxt)
        (register-file date))
      (println "Already processed today"))))

(defn -main
  "Entry point"
  []
  (def c (-> (conf/spark-conf)
             (conf/master "local[1]")
             (conf/app-name "funda-crawler")))

  (def sc (f/spark-context c))

  (p/retry
   {:strategy (p/progressive-retry-strategy :initial-delay 200, :max-delay 10000)}
   (p/retriable
    {}
    (run-batch sc)))

  (Thread/sleep (* 1000 60 60 24)))