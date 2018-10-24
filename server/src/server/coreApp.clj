(ns server.coreApp
  (:require [flambo.conf :as conf]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clj-time.local :as l]
            [flambo.api :as f]
            [flambo.tuple :as ft]
            [perseverance.core :as p]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clj-jgit.porcelain :as g]
            [clojure.java.shell :as shell]
            [server
             [request :refer [get-token nr-of-pages house-ids house-details]]]))

(def overview-file "../docs/generated/overview.json")
(def min-houses-per-postalcode 2)

(defn register-file
  ""
  [date]
  (let [readjson (-> overview-file
                     (slurp)
                     json/read-str)
        {dates "dates"} readjson
        updatejson (->
                    readjson
                    (assoc "dates" (conj dates date))
                    json/write-str)]
    (spit overview-file updatejson)))

;; Wrapper to run git command in terminal
(defn git-cmd
  [& args]
  (let [r (apply shell/sh "git" args)]
    (when (zero? (:exit r))
      (clojure.string/trim-newline (:out r)))))


(defn calculate-average-pricepersqm
  [inResult]
  (->
   inResult
   (f/group-by (f/fn [{postcode :postcode}] postcode))
     ;; Change from spark tuple to clojure hashmap and average
   (f/map
    (ft/key-val-fn
     (f/fn [k v]
       (let [pricepersqm
             (reduce
              (fn
                [{sqm :s
                  count :c
                  result :r}
                 {woonoppervlakte :woonoppervlakte
                  vraagprijs :vraagprijs}]
                (if (> woonoppervlakte 12)
                  (let [newSqm (+ sqm (/ vraagprijs woonoppervlakte))
                        newCount (inc count)]
                    {:s newSqm
                     :c newCount
                     :r (/ newSqm newCount)})
                  {:s sqm
                   :c count
                   :r result}))
              {:s 0
               :c 0
               :r 0}
              v)]
         (ft/tuple
          k
          pricepersqm)))))
          ;; Remove results with to little results
   (f/filter
    (ft/key-val-fn
     (f/fn [_ v] (> (:c v) min-houses-per-postalcode))))))

(defn fill-json
  [inResult]
  (let [result (calculate-average-pricepersqm inResult)
        objresult (apply
                   merge
                   (->
                    result
                    (f/map (ft/key-val-fn (f/fn [k v] {k v})))
                        ;; Collect result from spark
                    f/collect))
        minprice (->
                  result
                  (f/fold
                   Integer/MAX_VALUE
                   (f/fn [acc row]
                     (if (number? row) ;; Reduce can work in parallel, row can be a number
                       (min row acc)
                       (if (> (:c (._2 row)) 0) ;; If result is empty
                         (min (:r (._2 row)) acc)
                         acc)))))
        maxprice (->
                  result
                  (f/fold
                   Integer/MIN_VALUE
                   (f/fn [acc row]
                     (if (number? row) ;; Reduce can work in parallel, row can be a number
                       (max row acc)
                       (if (> (:c (._2 row)) 0) ;; If result is empty
                         (max (:r (._2 row)) acc)
                         acc)))))]
    json/write-str
    {:postcodes objresult
     :minprice minprice
     :maxprice maxprice}))

(defn results-to-file
  [inResult filename]
  (println "Write to file " filename)
  (spit
   (str filename ".json")
   (fill-json inResult))
   (git-cmd "add" (str filename ".json")))

(defn run-batch
  "Process the data from funda through Spark"
  [sc]
  (let [date (l/format-local-time (l/local-now) :date)
        filename (str "../docs/generated/" date)]
    (if-not (.exists (io/as-file filename))
      (let [token (get-token)
            result (->
                    (f/parallelize sc (range 1 3)) ;(nr-of-pages token)))
                    ;; Go through the list of houses available
                    (f/flat-map (f/iterator-fn [page] (house-ids token page)))
                    ;; Retrieve each house
                    (f/map (f/fn [x] (house-details token x)))
                    ;; Remove houses that are not included
                    (f/filter (f/fn [x] (some? x))))]
          ;; Write all results to file
        (results-to-file
         result
         date)
          ;; Filter on number of files
        (for [aantalSlaapkamers (range 1 5)]
          (results-to-file
           (f/filter
            result
            (ft/key-val-fn
             (f/fn [_ v] (= (:aantalslaapkamers v) aantalSlaapkamers))))
           (str date ".slaap." aantalSlaapkamers)))
          ;; Add to list of generated files
        (register-file date)
          ;; Add file to git version system
        (git-cmd "commit" "-am" (str "Generated " date))
        (git-cmd "push"))
      (println "Already processed today"))))

(defn -main
  "Entry point"
  []
  (def c (-> (conf/spark-conf)
             (conf/master "local[*]")
             (conf/app-name "funda-crawler")))

  (def sc (f/spark-context c))

  (try
    (p/retry
     {:strategy (p/progressive-retry-strategy :initial-delay 200, :max-delay 10000)}
     (p/retriable
      {}
      (run-batch sc)))
    (catch Exception e
      (do
        (println "Batch failed")
        (println e)))))
