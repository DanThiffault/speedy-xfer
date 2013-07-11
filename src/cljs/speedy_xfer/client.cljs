(ns speedy-xfer.client
  (:require [speedy-xfer.client.s3 :as s3]
             [domina :refer [by-id value by-class set-value! set-attr!
                            get-attr set-text! append! destroy! log nodes
                             single-node set-style! add-class! remove-class!]]
            [domina.xpath :refer [xpath]]
            [domina.css :as css]
            [domina.events :as events]
            [clojure.browser.repl :as repl]))


(defn add-download-link [container url]
  (let [tag (single-node (css/sel container "a"))]
    (set-text! tag "download")
    (set-attr! tag :href url)
    (events/unlisten! tag :click)))

(defn file-copied-handler [data]
  (let [container (single-node (xpath (str "//td[@data-region='" (:region data) "']")))]
    (add-download-link container (:url data))))

(defn generate-link-handle [e]
  (events/prevent-default e)
  (let [tag (events/target e)
        src-bucket (:original-bucket @s3/current-file)
        dest-region (.getAttribute (single-node (xpath tag "ancestor::td")) (name :data-region))
        file-name (:file-name @s3/current-file)]
    (set-text! tag "Transfering file to region...")
    (set-attr! tag :href "")
    (s3/copy-to-destination src-bucket dest-region file-name file-copied-handler)))

(defn reset-download-link [container]
  (let [tag (single-node (css/sel container "a"))]
    (set-text! tag "generate")
    (set-attr! tag :href "#generate")
    (events/listen! tag :click generate-link-handle)))

(defn upload-complete-handler []
  (doall
   (map #(if (= (:original-region-url @s3/current-file) (.getAttribute % (name :data-region)))
           (add-download-link % (:public-url @s3/current-file))
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
  (events/listen! (by-id "files") :change handle-file-select))

(set! (.-onload js/window) init)
