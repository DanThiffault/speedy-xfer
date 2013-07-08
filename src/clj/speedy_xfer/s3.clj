(ns speedy-xfer.s3
  (:use [environ.core :only [env]]
        [ring.util.codec :only [base64-encode]]
        [clj-time.core  :only [hours from-now]]
        [clj-time.format :only [unparse formatters]])
  (:require [clojure.string :as s]
            [clojure.data.json :as json])
  (:import (javax.crypto Mac
                         spec.SecretKeySpec)))

(def s3-bucket-suffix "uploads")

(def regions [["US East" "s3.amazonaws.com"]
              ;["Oregon" "s3-us-west-2.amazonaws.com"]
              ;["N. California" "s3-us-west-1.amazonaws.com"]
              ;["Ireland" "s3-eu-west-1.amazonaws.com"]
              ["Singapore" "s3-ap-southeast-1.amazonaws.com"]
              ;["Sydney" "s3-ap-southeast-2.amazonaws.com"]
              ;["Tokyo" "s3-ap-northeast-1.amazonaws.com"]
              ;["Brazil" "s3-sa-east-1.amazonaws.com"]
              ])

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
