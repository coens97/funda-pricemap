(ns server.request
  (:require [clj-uuid :as uuid]
            [org.httpkit.client :as http]
            [clojure.core]
            [clojure.data.json :as json]))

(def pagesize 25)
(defn parse-int [s]
  (try (Integer. (re-find  #"\d+" s))
       (catch Exception e nil)))

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
  ;(println (str "Loading page " page))
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
       ;; clojure.pprint/pprint
            ;; Only take GlobalId attribute
       (map (fn [{globalid "GlobalId"}] globalid))
            ;; Remove nils
       (filter (fn [x] (some? x)))))))

(defn house-details
  "Get details of house"
  [token globalid]
  ;; Functions to traverse the JSON file
  (defn get-list-by-title
    ""
    [detailtitle list]
    (let [{returnlist "List"} (->> list
                                   (filter (fn [{title "Title"}] (= title detailtitle)))
                                   first)]
      returnlist))
  (defn get-value-by-label
    ""
    [label list]
    (let [{returnvalue "Value"} (->> list
                                     (filter (fn [{l "Label"}] (= l label)))
                                     first)]
      returnvalue))

  ;(println (str "Loading house detail " globalid))
  (let
   [{:keys [status headers body error] :as resp}
    @(http/get
      (str "https://mobile.funda.io/api/v1/Aanbod/Detail/Koop/" globalid "/GekliktAppResultaatlijst")
      {:user-agent "Funda/74 CFNetwork/902.2 Darwin/17.7.0" ;; Impersonate iPhone App
       :headers {"Accept" "*/*"
                 "accepted_cookie_policy" "12"
                 "Cookie" "X-Stored-Data=null"
                 "Accept-Language" "nl-NL"
                 "api_key" token}})]
    (if error
        ;; Failed
      (do (println "Failed, exception is " error)
          [])
        ;; Success
      (let [json (json/read-str body)
            {housedatalist "List"} (first json)
            overdrachtlist (get-list-by-title "Overdracht" housedatalist)
            parsedvraagprijs (-> (get-value-by-label "Vraagprijs" overdrachtlist)
                                 (clojure.string/replace "." "")
                                 parse-int)
            status (get-value-by-label "Status" overdrachtlist)
            bouwlist (get-list-by-title "Bouw" housedatalist)
            bouwjaar (get-value-by-label "Bouwjaar" bouwlist)
            typehuis (get-value-by-label "Soort woonhuis" bouwlist)
            bouwvorm (get-value-by-label "Bouwvorm" bouwlist)
            opervlaklist (get-list-by-title "Oppervlakten en inhoud" housedatalist)
            gebruikoppervlaklist (get-list-by-title "Gebruiksoppervlakten" opervlaklist)
            woonoppervlakte (parse-int (get-value-by-label "Wonen (= woonoppervlakte)" gebruikoppervlaklist))
            perceeloppervlakte (parse-int (get-value-by-label "Perceeloppervlakte" opervlaklist))
            indelinglist (get-list-by-title "Indeling" housedatalist)
            [_ aantalkamers aantalslaapkamers] (re-matches #"(\d+) kamers \((\d+).*" (get-value-by-label "Aantal kamers" indelinglist))
            aantalkamersparsed (parse-int aantalkamers)
            aantalslaapkamersparsed (parse-int aantalslaapkamers)
            kadastralegegevenslist (get-list-by-title "Kadastrale gegevens" housedatalist)
            {postcode "Title"} (first kadastralegegevenslist)
            postcodeparsed (parse-int postcode)]
        ;; (println status)(println parsedvraagprijs)(println bouwjaar)(println typehuis)(println bouwvorm)(println woonoppervlakte)(println perceeloppervlakte)(println aantalkamers)(println aantalslaapkamers)(println postcodeparsed)
        (if (and (some? postcodeparsed) (= status "Beschikbaar") (= bouwvorm "Bestaande bouw"))
          {:vraagprijs parsedvraagprijs
           :typehuis typehuis
           :woonoppervlakte woonoppervlakte
           :perceeloppervlakte perceeloppervlakte
           :aantalkamers aantalkamersparsed
           :aantalslaapkamers aantalslaapkamersparsed
           :postcode postcodeparsed})))))

