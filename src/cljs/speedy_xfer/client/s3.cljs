(ns speedy-xfer.client.s3
  (:require [shoreleave.remotes.http-rpc :refer [remote-callback]]))

(def access-key (atom ""))

(defn retrieve-access-key []
  (remote-callback :access-key
                   []
                   #(reset! access-key (:access-key %))))

(defn sign-file-path [file region f]
  (remote-callback :sign-url
                   [region (.-name file)]
                   f))



(defn generate-form-data [filekey policy signature file]
  (let [fd (js/FormData.)]
    (.append fd "key" filekey)
    (.append fd "AWSAccessKeyId" @access-key)
    (.append fd "acl" "public-read")
    (.append fd "policy" policy)
    (.append fd "signature" signature)
    (.append fd "success_action_status" "201")
    (.append fd "file" file)
    fd))

(defn upload-signed-file [url filekey policy signature file progress-handler]
  (let [xhr (js/XMLHttpRequest.)]
    (.open xhr "POST" url true)
    (set! (.-onerror xhr) #(set! (.-sslast js/window) %))
    (set! (.-onprogress (.-upload xhr)) progress-handler)
    (.send xhr (generate-form-data filekey policy signature file))))


(defn file-list-to-array [file-list]
  (reduce (fn [a b] (conj a (.item file-list b))) [] (range (.-length file-list))))


(defn upload-file [file region progress-handler]
  (sign-file-path file region #(upload-signed-file (:target-url %) (:key %) (:policy %) (:signature %) file progress-handler)))

(defn upload-files [file-list region progress-handler]
  (let [files (file-list-to-array file-list)]
    (doseq [file files]
      (upload-file file region progress-handler))))

(defn init []
  (retrieve-access-key))
