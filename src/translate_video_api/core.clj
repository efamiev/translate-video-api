(ns translate-video-api.core
  (:gen-class)
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [GET POST ANY defroutes]]
            [translate-video-api.translator :refer [translate]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.logger]
            [clojure.tools.logging :as logging]))

(defroutes app-routes
  (GET "*" request
    (let [{:keys [uri request-method]} request]
      {:status 200
       :headers {"Content-Type" "text/plain"}
       :body (format "You requested %s %s" (-> request-method name .toUpperCase) uri)}))

  (POST "/api/initialize_translation" request
    ; Create logs middleware
    (logging/info "REQUEST BODY:" (get request :body))
    (let [{:keys [body]} request
          res {:translation_status "translation_processing"}
          link (get body "link")]

      (translate ["vot-cli" link] {:chat-id (get body "chat_id") :link link} 10000)

      (logging/info "RESPONSE:" res)
      {:status 200 :body res}))

  ; (POST "/api/subs" request
  ;   (let [{:keys [body]} request]
  ;   {:status 200
  ;    :body {:download_link (translator/run-script (:link body))}}))

  (ANY "*" _ {:status 404
              :headers {"content-type" "text/plain"}
              :body "Content not found."}))

(def app (-> app-routes
             wrap-reload
             wrap-json-response
             wrap-json-body
             ring.logger/wrap-with-logger))

(defonce server (atom nil))

(defn start! []
  (reset! server (run-jetty app {:port 8080 :join? false})))

(defn -main [& args]
  (start!))

(comment
  (.stop @server)
  (start!))















