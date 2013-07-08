(ns speedy-xfer
  (:require [domina :refer [by-id value by-class set-value! set-attr!
                            set-text! append! destroy! log]]
            [domina.events :refer [listen!]]
            [clojure.browser.repl :as repl]
            [shoreleave.remotes.http-rpc :refer [remote-callback]]
            [cljs.reader :refer [read-string]]
            [goog.net.XhrIo :as gxhrio]))


(defn generate-form-data [filekey policy signature file]
  (let [fd (js/FormData.)]
    (.append fd "key" filekey)
    (.append fd "AWSAccessKeyId" "1WH6H3JANFZSYAJXG5G2")
    (.append fd "acl" "public-read")
    (.append fd "policy" policy)
    (.append fd "signature" signature)
    (.append fd "success_action_status" "201")
    (.append fd "file" file)
    fd))

(defn sign-file-path [file f]
  (remote-callback :sign-url
                   [(.-value (by-id "region-from")) (.-name file)]
                   f))


(defn upload-progress-handler [e data]
  (let [done (.-loaded e)
        total (.-total e)
        progress-bar (by-id "file-progress")]
    (set! (.-max progress-bar) total)
    (set! (.-value progress-bar) done)))

(defn upload-signed-file [url filekey policy signature file]
  (let [xhr (js/XMLHttpRequest.)]
    (.open xhr "POST" url true)
    (set! (.-onerror xhr) #(set! (.-sslast js/window) %))
    (set! (.-onprogress (.-upload xhr)) upload-progress-handler)
    (.send xhr (generate-form-data filekey policy signature file))))


(defn file-list-to-array [file-list]
  (reduce (fn [a b] (conj a (.item file-list b))) [] (range (.-length file-list))))


(defn set-progress [progress msg]
  (set-attr! (by-id "file-progress") :value progress)
  (set-text! (by-id "status") msg))

(defn upload-file [file]
  (sign-file-path file #(upload-signed-file (:target-url %) (:key %) (:policy %) (:signature %) file)))

(defn upload-files [file-list]
  (let [files (file-list-to-array file-list)]
    (doseq [file files]
      (upload-file file))))

(defn handle-file-select [evt]
  (set-progress 0 "Uploading file")
  (upload-files (-> evt :target .-files)))

(defn ^:export init []
  (repl/connect "http://localhost:9000/repl")
  (listen! (by-id "files") :change handle-file-select))

(set! (.-onload js/window) init)
