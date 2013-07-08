(ns speedy-xfer.server
  (:use [environ.core :only [env]])
  (:require [speedy-xfer.s3 :as s3]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.resource :as resources]
            [ring.middleware.reload :as reload]
            [ring.util.response :as response]
            [hiccup.page :as html]
            [hiccup.form :as form]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [resources not-found]]
            [compojure.handler :refer [site]]
            [shoreleave.middleware.rpc :refer [defremote wrap-rpc]])
  (:import (javax.crypto Mac
                         spec.SecretKeySpec))
  (:gen-class))


(defn generate-dest-region-row [[description url]]
  [:tr
   [:td description]
   [:td {:data-region url :class "dest-region"} [:a {:href "#"} ""]]])

(defn generate-dest-regions []
  [:tbody
  (map generate-dest-region-row s3/regions)])

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
                   (form/select-options s3/regions "s3.amazonaws.com")]]
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


(defroutes app-routes
  (GET "/" [] (upload-page))
  (resources "/")
  (not-found "Page not found"))

(defremote sign-url [region-url file-name] (s3/generate-signed-url s3/cred region-url (get s3/regional-buckets region-url) file-name))

(defremote access-key [] {:access-key (:access-key s3/cred)})


(def handler
  (site app-routes))

(def app (-> (var handler)
             (wrap-rpc)
             (site)))

(defn -main [& args]
  (jetty/run-jetty app {:port 3000}))

