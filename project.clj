(defproject speedy-xfer "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.2.0"]
                 [clj-time "0.5.0"]
                 [ring "1.1.8"]
                 [hiccup "1.0.3"]
                 [domina "1.0.1"]
                 [environ "0.4.0"]
                 [clj-aws-s3 "0.3.5"]
                 [ring-edn "0.1.0"]
                 [shoreleave/shoreleave-remote-ring "0.3.0"]
                 [shoreleave/shoreleave-remote "0.3.0"]]
  :plugins [[lein-cljsbuild "0.3.0"]
            [lein-ring "0.8.3"]]
  :hooks [leiningen.cljsbuild]
  :source-paths ["src/clj"]
  :cljsbuild {
    :builds {
      :main {
        :source-paths ["src/cljs"]
        :compiler {:output-to "resources/public/js/cljs.js"
                   :optimizations :simple
                   :pretty-print true}
        :jar true}}}
  :main speedy-xfer.server
  :ring {:handler speedy-xfer.server/app})

