(ns server.spouts
  "Spouts.

More info on the Clojure DSL here:

https://github.com/nathanmarz/storm/wiki/Clojure-DSL"
  (:require [clj-uuid :as uuid]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [backtype.storm [clojure :refer [defspout spout emit-spout!]]]))

(defspout token-spout ["token"]
  [conf context collector]
  (spout
   (nextTuple
    []
      ;; Make HTTP request in order to get token needed to make further API requests
    (http/get
     (str "https://mobile.funda.io/api/v1/Token/" (uuid/v1))
     {:user-agent "Funda/74 CFNetwork/902.2 Darwin/17.7.0" ;; Impersonate iPhone App
      :headers {"Accept" "*/*"
                "Cookie" "X-Stored-Data=null"
                "Accept-Language" "nl-NL"}}
     (fn [{:keys [status headers body error]}] ;; asynchronous response handling
       (if error
            ;; Failed
         (println "Failed, exception is " error)
           ;; Success
         (do
           (println "Async HTTP GET: " status)
             ;; Parse Json response
           (let [jsonparsed (json/read-str body)
                 {token "Token"} jsonparsed]
             (println (str "Got token: " token))
             (emit-spout! collector [token]))))))
    ;; (emit-spout! collector [(rand-nth stormys)])
    (Thread/sleep (* 1000 60 60 24)))
   (ack [id]
        ;; You only need to define this method for reliable spouts
        ;; (such as one that reads off of a queue like Kestrel)
        ;; This is an unreliable spout, so it does nothing here
)))
