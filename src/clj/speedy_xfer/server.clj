(ns speedy-xfer.server
  (:use [environ.core :only [env]]
        [ring.util.codec :only [base64-encode]]
        clj-time.core
        clj-time.format)
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.resource :as resources]
            [ring.middleware.reload :as reload]
            [ring.util.response :as response]
            [hiccup.page :as html]
            [hiccup.form :as form]
            [aws.sdk.s3 :as s3]
            [clojure.string :as s]
            [clojure.data.json :as json]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [resources not-found]]
            [compojure.handler :refer [site]]
            [shoreleave.middleware.rpc :refer [defremote wrap-rpc]])
  (:import (javax.crypto Mac
                         spec.SecretKeySpec))
  (:gen-class))

(def s3-bucket-suffix "uploads")

(def regions [["US East" "s3.amazonaws.com"]
              ["Oregon" "s3-us-west-2.amazonaws.com"]
              ["N. California" "s3-us-west-1.amazonaws.com"]
              ["Ireland" "s3-eu-west-1.amazonaws.com"]
              ["Singapore" "s3-ap-southeast-1.amazonaws.com"]
              ["Sydney" "s3-ap-southeast-2.amazonaws.com"]
              ["Tokyo" "s3-ap-northeast-1.amazonaws.com"]
              ["Brazil" "s3-sa-east-1.amazonaws.com"]])

(def regional-buckets
  {"s3.amazonaws.com" "speedyxfer-us-east-1"
   "s3-us-west-2.amazonaws.com" "speedyxfer-ap-southeast-1"
   "s3-us-west-1.amazonaws.com" "speedyxfer-ap-southeast-1"
   "s3-eu-west-1.amazonaws.com" "speedyxfer-ap-southeast-1"
   "s3-ap-southeast-1.amazonaws.com" "speedyxfer-ap-southeast-1"
   "s3-ap-southeast-2.amazonaws.com" "speedyxfer-ap-southeast-1"
   "s3-ap-northeast-1.amazonaws.com" "speedyxfer-ap-southeast-1"
   "s3-sa-east-1.amazonaws.com" "speedyxfer-ap-southeast-1"})

(def s3-cred
  {:secret-key (env :s3-secret)
   :access-key (env :s3-key)})

(defn generate-dest-region-row [[description url]]
  [:tr
   [:td description]
   [:td [:a {:href "#"} "Generate"]]])

(defn generate-dest-regions []
  [:tbody
  (map generate-dest-region-row regions)])

(defn upload-page []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body
   (html/html5 [:head
               [:title "Speedy Transfer"]
               (html/include-css "/css/bootstrap.css")
                (html/include-js "/js/cljs.js")]
              [:body
               [:div.container
                [:div.page-header
                 [:h1 "Speedy Transfer"]]
                [:div.row
                 [:div.span4
                  [:label {:for :region-from} "Closest to you"]
                  [:select {:id :region-from :name :region-from}
                   (form/select-options regions "s3.amazonaws.com")]]
                 [:div.span4
                  [:input {:id :files :type :file :multiple :yes :style "margin-top: 2em"}]]]
                [:div.row
                 [:div.span8
                  [:progress {:id :file-progress :style "width: 100%;" :value 0}]]]
                [:div.row
                 [:div.span8
                  [:span#status]]]
                [:dev.row
                 [:div.span8
                   [:table {:class "table"}
                    [:thead
                     [:tr [:th "Region"] [:th "link"]]]
                    (generate-dest-regions)]]]
                ]])})

(defn generate-policy-document [bucket bucket-suffix] (s/replace (json/write-str
                            {:expiration (->> 24 hours from-now (unparse (formatters :date-time-no-ms )))
                             :conditions [
                                          {:bucket bucket}
                                          {:acl "public-read"}
                                          ["starts-with" "$key" (str bucket-suffix "/")]
                                          {:success_action_status "201"}
                                          ["content-length-range" 0 110485760]]
                             })
                           #"\n|\r"
                           ""))

(defn generate-policy [policy-json]
  (s/replace
   (base64-encode (.getBytes  policy-json "UTF-8"))
   #"\n|\r" ""))

(defn generate-signature [secret-key policy]
  (let [hmac (Mac/getInstance "HmacSHA1")]
    (.init hmac (SecretKeySpec. (.getBytes (:secret-key s3-cred) "UTF-8")
                                "HmacSHA1"))
    (s/replace
     (base64-encode (.doFinal hmac (.getBytes policy "UTF-8")))
     #"\n|\r" "")))

(defn generate-signed-url [cred region-url bucket filename]
  (let [policy (generate-policy (generate-policy-document bucket s3-bucket-suffix))]
     {:key (str s3-bucket-suffix "/" filename)
      :policy policy
      :signature (generate-signature (:secret-key cred) policy)
      :target-url (str "https://" bucket "." region-url "/")}))


(defroutes app-routes
  (GET "/" [] (upload-page))
  (resources "/")
  (not-found "Page not found"))

(defremote sign-url [region-url file-name] (generate-signed-url s3-cred region-url (get regional-buckets region-url) file-name))

(def handler
  (site app-routes))

(def app (-> (var handler)
             (wrap-rpc)
             (site)))

(defn -main [& args]
  (jetty/run-jetty app {:port 3000}))

