(ns speedy-xfer.client
  (:require [speedy-xfer.client.s3 :refer [upload-files]]
             [domina :refer [by-id value by-class set-value! set-attr!
                            set-text! append! destroy! log]]
            [domina.events :refer [listen!]]
            [clojure.browser.repl :as repl]))


(defn upload-progress-handler [e data]
  (let [done (.-loaded e)
        total (.-total e)
        progress-bar (by-id "file-progress")]
    (set! (.-max progress-bar) total)
    (set! (.-value progress-bar) done)))

(defn handle-file-select [evt]
  (upload-files (-> evt :target .-files) (.-value (by-id "region-from")) upload-progress-handler))

(defn ^:export init []
  (repl/connect "http://localhost:9000/repl")
  (listen! (by-id "files") :change handle-file-select))

(set! (.-onload js/window) init)
