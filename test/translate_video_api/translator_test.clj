(ns translate-video-api.translator-test
  (:require [clojure.test :refer [deftest testing is]]
            [translate-video-api.translator :refer [translate send-translate-link]]
            [clj-http.client :as client]))

(defmacro with-send-translate-link
  [& body]
  `(with-redefs [send-translate-link (fn [~'cmd ~'chat-id] "ok")]
     ~@body))

;TODO: Make a vot-cli stub
(deftest test-translate
  (with-send-translate-link
    (testing "Return a link to the translated audio"
      (let [cmd ["vot-cli" "https://youtube.com/shorts/ABbrWOmvgRU?si=pCQjpCOE4lgWsWek"]
            [process future-task] (translate cmd 1000 1)
            {:keys [link errors]} @future-task]
        (is (= [] errors))
        (is (boolean (re-matches #"^https://vtrans\.s3-private\.mds\.yandex\.net.*$" link)))
        (is (not (.isAlive process)))))

    (testing "Return an error if an invalid link is provided"
      (let [cmd ["vot-cli" "https://ube.com/shorts/ABbrWOmvgRU?si=pCQjpCOE4lgWsWek"]
            [process future-task] (translate cmd 1000 1)]
        (is (= {:link "" :errors ["URL: https://ube.com/shorts/ABbrWOmvgRU?si=pCQjpCOE4lgWsWek is unknown service"]} @future-task))
        (is (not (.isAlive process)))))

    (testing "Terminate the process if the wait exceeds the timeout"
      (let [cmd ["sleep" "100000"] [process future-task] (translate cmd 100 1)]
        (is (.isAlive process))
        (is (= {:link "" :errors ["Timeout"]} @future-task))

      ; (Thread/sleep 100)
        (is (not (.isAlive process)))
        (is (future-done? future-task))))))
