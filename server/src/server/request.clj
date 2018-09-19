(ns server.request
  (:require [clj-uuid :as uuid]
            [org.httpkit.client :as http]
            [clojure.core]
            [clojure.data.json :as json]))

(def pagesize 25)
(defn parse-int [s]
  (Integer. (re-find  #"\d+" s)))

(defn get-token
  "Make request to Funda to get API token to be used with further requesrts"
  []
  (let
   [{:keys [status headers body error] :as resp}
    @(http/get (str "https://mobile.funda.io/api/v1/Token/" (uuid/v1))
               {:user-agent "Funda/74 CFNetwork/902.2 Darwin/17.7.0" ;; Impersonate iPhone App
                :headers {"Accept" "*/*"
                          "Cookie" "X-Stored-Data=null"
                          "Accept-Language" "nl-NL"}})]
    (if error
        ;; Failed
      (do
        (println "Failed, exception is " error)
        nil)
        ;; Success
      (do
        (println "Sync HTTP GET: " status)
        ;; Parse Json response
        (let [jsonparsed (json/read-str body)
              {token "Token"} jsonparsed]
          (println (str "Got token: " token))
          token)))))

(defn house-list
  "Make request to retrieve the list of all housing listings"
  [token page]
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
               "api_key" token}}))

(defn nr-of-pages
  "Number of pages with housing listings"
  [token]
  (let
   [{:keys [status headers body error] :as resp}
    (house-list token 1)]
    (if error
            ;; Failed
      (do (println "Failed, exception is " error)
          0)
           ;; Success
      (let [{total-countstring :x-total-count} headers ;; Number of houses listed in string
            total-count (parse-int total-countstring)] ;; Number of houses to integer
        (quot total-count pagesize)))))

(defn house-ids
  "Number of pages with housing listings"
  [token page]
  (let
   [{:keys [status headers body error] :as resp}
    (house-list token page)]
    (if error
        ;; Failed
      (do (println "Failed, exception is " error)
          [])
        ;; Success
      (->>
       (json/read-str body)
            ;; Only take GlobalId attribute
       (map (fn [{globalid "GlobalId"}] globalid))
            ;; Remove nils
       (filter (fn [x] (some? nil)))))))