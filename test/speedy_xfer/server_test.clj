(ns speedy-xfer.server-test
  (:use clojure.test
        speedy-xfer.server))

(deftest generate-signed-url-test
  (testing "generate with valid filename"
    (is (.startsWith (generate-signed-url s3-cred bucket-name "my-file") "https://speedyxfer-ap-southeast-1.s3.amazonaws.com/my-file")))
  (testing "handle bad input"))

;(run-tests 'speedy-xfer.server-test)

