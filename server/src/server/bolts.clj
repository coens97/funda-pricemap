(ns server.bolts
  "Bolts.

More info on the Clojure DSL here:

https://github.com/nathanmarz/storm/wiki/Clojure-DSL"
  (:require [org.httpkit.client :as http]
            [clojure.core]
            [clojure.data.json :as json]
            [backtype.storm [clojure :refer [emit-bolt! defbolt ack! bolt]]]))

(defbolt stormy-bolt ["stormy"] [{type :type :as tuple} collector]
  (emit-bolt! collector [(case type
                           :regular "I'm regular Stormy!"
                           :bizarro "I'm bizarro Stormy!"
                           "I have no idea what I'm doing.")]
              :anchor tuple)
  (ack! collector tuple))

(defbolt server-bolt ["message"] [{stormy :stormy :as tuple} collector]
  (emit-bolt! collector [(str "server produced: " stormy)] :anchor tuple)
  (ack! collector tuple))

(defbolt houselist-bolt ["globalid"] [{token :token :as tuple} collector]
  ;;(println token)
  (http/get
   (str "https://mobile.funda.io/api/v1/Aanbod/koop/heel-nederland%252Fbeschikbaar%252F?page=" 1 "&pageSize=25&compact=True")
   {:user-agent "Funda/74 CFNetwork/902.2 Darwin/17.7.0" ;; Impersonate iPhone App
    :headers {"Accept" "*/*"
              "Cookie" "X-Stored-Data=null"
              "Accept-Language" "nl-NL"
              "api_key" token}}
   (fn [{:keys [status headers body error]}] ;; asynchronous response handling
     (if error
            ;; Failed
       (println "Failed, exception is " error)
           ;; Success
       (let [jsonparsed (json/read-str body)
             {total-count :x-total-count} headers]
         (println total-count)
         (->>
          jsonparsed
            ;; Only take GlobalId attribute
          (map (fn [{globalid "GlobalId"}] (#(identity [%]) globalid)))
            ;; Remove nils
          (filter (fn [x] (not= x [nil])))
            ;; Emit house id to other bolt
          (mapv (fn [x] (emit-bolt! collector x :anchor tuple))))))))
  (ack! collector tuple))

(defbolt house-bolt ["something"] [{globalid :globalid :as tuple} collector]
  (println (str "Got houseid:" globalid))
  (ack! collector tuple))
