(ns server.spouts
  "Spouts.

More info on the Clojure DSL here:

https://github.com/nathanmarz/storm/wiki/Clojure-DSL"
  (:require [clj-uuid :as uuid]
            [org.httpkit.client :as http]
            [backtype.storm [clojure :refer [defspout spout emit-spout!]]]))

(defspout type-spout ["type"]
  [conf context collector]
  (let [stormys [:regular :bizarro]]
    (spout
     (nextTuple []
                (http/get
                 (str "https://mobile.funda.io/api/v1/Token/" (uuid/v1))
                 {:user-agent "Funda/74 CFNetwork/902.2 Darwin/17.7.0"
                  :headers {"Cookie" "X-Stored-Data=null"
                                  ;; "User-Agent" "Funda/74 CFNetwork/902.2 Darwin/17.7.0"
                            "Accept-Language" "nl-NL"}}
                 (fn [{:keys [status headers body error]}] ;; asynchronous response handling
                   (println body)
                   (if error
                     (println "Failed, exception is " error)
                     (println "Async HTTP GET: " status))))
                (emit-spout! collector [(rand-nth stormys)])
                (Thread/sleep (* 1000 60 60 24)))
     (ack [id]
        ;; You only need to define this method for reliable spouts
        ;; (such as one that reads off of a queue like Kestrel)
        ;; This is an unreliable spout, so it does nothing here
))))
