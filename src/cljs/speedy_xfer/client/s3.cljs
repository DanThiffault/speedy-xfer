(ns speedy-xfer.client.s3
  (:require [shoreleave.remotes.http-rpc :refer [remote-callback]]))

(def access-key (atom ""))

(def current-file (atom {}))

(defn retrieve-access-key []
  (remote-callback :access-key
                   []
                   #(reset! access-key (:access-key %))))

(defn sign-file-path [file region f]
  (remote-callback :sign-url
                   [region (.-name file)]
                   f))

(defn copy-to-destination [src-bucket dest-region fkey f]
  (remote-callback :copy-to-destination
                   [src-bucket dest-region fkey]
                   f))

(defn generate-form-data [filekey policy signature file]
  (let [fd (js/FormData.)]
    (.append fd "key" filekey)
    (.append fd "AWSAccessKeyId" @access-key)
    (.append fd "acl" "bucket-owner-read")
    (.append fd "policy" policy)
    (.append fd "signature" signature)
    (.append fd "success_action_status" "201")
    (.append fd "file" file)
    fd))

(defn upload-signed-file [url filekey policy signature file progress-handler complete-handler]
  (let [xhr (js/XMLHttpRequest.)]
    (.open xhr "POST" url true)
    (set! (.-onerror xhr) #(set! (.-sslast js/window) %))
    (set! (.-onprogress (.-upload xhr)) progress-handler)
    (set! (.-onload xhr) complete-handler)
    (.send xhr (generate-form-data filekey policy signature file))))

(defn file-list-to-array [file-list]
  (reduce (fn [a b] (conj a (.item file-list b))) [] (range (.-length file-list))))

(defn signed-file-handler [file progress-handler complete-handler response]
  (reset! current-file {:original-bucket (:bucket response) :key (:key response) :original-region-url (:region-url response) :target-url (:target-url response) :public-url (:public-url response) :file-name (:file-name response)})
  (upload-signed-file (:target-url response) (:key response) (:policy response) (:signature response) file progress-handler complete-handler))

(defn upload-file [file region progress-handler upload-complete-handler]
  (sign-file-path file region (partial signed-file-handler file progress-handler upload-complete-handler)))

(defn upload-files [file-list region progress-handler upload-complete-handler]
  (let [files (file-list-to-array file-list)]
    (doseq [file files]
      (upload-file file region progress-handler upload-complete-handler))))

(defn init [] [] (retrieve-access-key))
