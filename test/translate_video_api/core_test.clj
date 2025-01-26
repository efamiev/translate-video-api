(ns translate-video-api.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [translate-video-api.core :as translate-video-api]
            [ring.mock.request :as mock]
            [translate-video-api.translator :refer [translate]]
            [cheshire.core :refer [generate-string]]))

(defmacro with-translate
  [result & body]
  `(with-redefs [translate (fn [~'cmd ~'timeout ~'chat-id] ~result)]
     ~@body))

(deftest test-app
  (testing "Отправляем положительный статус о начале перевода"
    (with-translate "ok"
      (let [request (-> (mock/request :post "/api/initialize_translation")
                        (mock/json-body (generate-string {:link "https://youtube.com/shorts/ABbrWOmvgRU?si=pCQjpCOE4lgWsWek" :chat_id 1})))
            response (translate-video-api/app request)
            {:keys [status body]} response]
        (is (= (generate-string {:translation_status "translation_processing"}) body))
        (is (= 200 status))))))
