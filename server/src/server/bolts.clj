(ns server.bolts
  "Bolts.

More info on the Clojure DSL here:

https://github.com/nathanmarz/storm/wiki/Clojure-DSL"
  (:require [org.httpkit.client :as http]
            [clojure.core]
            [clojure.data.json :as json]
            [backtype.storm [clojure :refer [emit-bolt! defbolt ack! bolt]]]))

(defn parse-int [s]
  (Integer. (re-find  #"\d+" s)))

(defbolt houselist-bolt ["globalid"] [{token :token :as tuple} collector]
  (def pagesize 25)
  (loop [page 1]
    (let
     [{:keys [status headers body error] :as resp}
      @(http/get
        (str
         "https://mobile.funda.io/api/v1/Aanbod/koop/heel-nederland%252Fbeschikbaar%252F?page="
         page
         "&pageSize="
         pagesize
         "&compact=True")
        {:user-agent "Funda/74 CFNetwork/902.2 Darwin/17.7.0" ;; Impersonate iPhone App
         :headers {"Accept" "*/*"
                   "Cookie" "X-Stored-Data=null"
                   "Accept-Language" "nl-NL"
                   "api_key" token}})]
      (if error
            ;; Failed
        (println "Failed, exception is " error)
           ;; Success
        (let [jsonparsed (json/read-str body)
              {total-countstring :x-total-count} headers ;; Number of houses listed in string
              total-count (parse-int total-countstring) ;; Number of houses to integer
              currentposition (* page pagesize)] ;; Number of houses that is retrieved after this iteration
          (println (str "Retrieving houselist: " currentposition "/" total-count))
          (->>
           jsonparsed
            ;; Only take GlobalId attribute
           (map (fn [{globalid "GlobalId"}] (#(identity [%]) globalid)))
            ;; Remove nils
           (filter (fn [x] (not= x [nil])))
            ;; Emit house id to other bolt
           (mapv (fn [x] (emit-bolt! collector x :anchor tuple))))
           ;; Continue the loop if more houses need to be retrieved
          (if (> total-count currentposition) (recur (inc page)))))))
  (ack! collector tuple))

(defbolt house-bolt ["something"] [{globalid :globalid :as tuple} collector]
  (println (str "Got houseid:" globalid))
  (ack! collector tuple))
