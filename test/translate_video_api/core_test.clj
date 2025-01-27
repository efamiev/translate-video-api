(ns translate-video-api.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [translate-video-api.core :as translate-video-api]
            [ring.mock.request :as mock]
            [translate-video-api.translator :refer [telegram-url]]
            [cheshire.core :refer [generate-string]]
            [ring.adapter.jetty :as jetty]
            [ring.logger]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.util.response :as response]
            [clojure.string :as str]))

(defmacro with-telegram-url
  [& body]
  `(with-redefs [telegram-url (str "http://localhost:8080/bot" (System/getenv "BOT_TOKEN") "/sendMessage")]
     ~@body))

(defonce server (atom nil))
(def processing-result (atom nil))

(defn webhook-handler [request]
  (reset! processing-result request)
  (response/response "Webhook received"))

(def app (-> webhook-handler
             wrap-reload
             wrap-json-response
             wrap-json-body))

(defn start-webhook-server []
  (reset! server
          (jetty/run-jetty app {:port 8080 :join? false})))

(defn stop-webhook-server []
  (when @server
    (.stop @server)
    (reset! server nil)))

(deftest test-app
  (start-webhook-server)

  (with-telegram-url
    (testing "Send a positive status indicating the start of the translation and send processing result to chat"
      (let [request (-> (mock/request :post "/api/initialize_translation")
                        (mock/json-body {:link "https://youtube.com/shorts/ABbrWOmvgRU?si=pCQjpCOE4lgWsWek" :chat_id 1 :lang "En" :reslang "En"}))
            response (translate-video-api/app request)
            {:keys [status body]} response]
        (is (= (generate-string {:translation_status "translation_processing"}) body))
        (is (= 200 status)))

      (Thread/sleep 1000)
      (let [text (-> @processing-result
                     :body
                     (get "text")
                     (str/split #" ")
                     butlast
                     vec)
            prefix "link\n\nhttps://vtrans.s3-private.mds.yandex.net"
            link-text (last (str/split (get (:body @processing-result) "text") #" "))]

        (is (= "Audio translation of the video https://youtube.com/shorts/ABbrWOmvgRU?si=pCQjpCOE4lgWsWek is located at the" (str/join " " text)))
        (is (str/starts-with? link-text prefix)))))

  (stop-webhook-server))
