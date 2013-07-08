(ns speedy-xfer.client
  (:require [speedy-xfer.client.s3 :as s3]
             [domina :refer [by-id value by-class set-value! set-attr!
                            get-attr set-text! append! destroy! log nodes
                             single-node set-style!]]
            [domina.css :as css]
            [domina.events :refer [listen!]]
            [clojure.browser.repl :as repl]))

(defn add-download-link [container url]
  (let [tag (single-node (css/sel container "a"))]
    (set-text! tag "download")
    (set-attr! tag :href url)))

(defn reset-download-link [container]
  (let [tag (single-node (css/sel container "a"))]
    (set-text! tag "generate")
    (set-attr! tag :href "")))

(defn ^:export upload-complete-handler []
  (log "upload complete")
  (doall
   (map #(if (= (:original-region-url @s3/current-file) (.getAttribute % (name :data-region)))
           (add-download-link % (str (:target-url @s3/current-file) (:key @s3/current-file)))
           (reset-download-link %))
        (nodes (css/sel ".dest-region"))))
  (set-style! (by-id "region-download-links") "display" "block"))

(defn upload-progress-handler [e data]
  (let [done (.-loaded e)
        total (.-total e)
        progress-bar (by-id "file-progress")]
    (set! (.-max progress-bar) total)
    (set! (.-value progress-bar) done)))

(defn handle-file-select [evt]
  (s3/upload-files (-> evt :target .-files) (.-value (by-id "region-from")) upload-progress-handler upload-complete-handler))

(defn ^:export init []
  ;(repl/connect "http://localhost:9000/repl")
  (s3/init)
  (listen! (by-id "files") :change handle-file-select))

(set! (.-onload js/window) init)
