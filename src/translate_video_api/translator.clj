(ns translate-video-api.translator
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :refer [info error]]
            [clj-http.client :as client]
            [cheshire.core :refer [generate-string]])
  (:import [java.lang ProcessBuilder]))

(defn remove-apostrophes [input]
  (str/replace input #"\"" ""))

(defn extract-vtrans-link [s]
  (remove-apostrophes (clojure.string/trim (last (clojure.string/split s #"\):")))))

(defn find-audio-link [lines]
  (extract-vtrans-link (some #(when (.startsWith % "Audio Link") %) lines)))

(defn send-translate-link [chat-id link]
  (client/post
   "https://api.telegram.org/bot{bot-token}/sendMessage"
   {:body (generate-string {:chat_id chat-id :text link}) :content-type :json}))

(defn message [])

(defn translate [cmd timeout chat-id]
  (let [process-builder (ProcessBuilder. cmd)
        process (.start process-builder)
        future-task (future
                      (try
                        (let [exit-code (.waitFor process timeout java.util.concurrent.TimeUnit/MILLISECONDS)]
                          (if (not exit-code)
                            ((fn []
                               (info "KILL PROCESS" cmd)
                               (.destroyForcibly  process)
                               {:link "" :errors ["Timeout"]}))
                            (let [stdout (with-open [r (clojure.java.io/reader (.getInputStream process))]
                                           (reduce (fn [acc line] (conj acc line)) [] (line-seq r)))
                                  stderr (with-open [r (clojure.java.io/reader (.getErrorStream process))]
                                           (reduce (fn [acc line] (conj acc line)) [] (line-seq r)))]

                              (if (empty? stderr)
                                ((fn []
                                   (let [link (find-audio-link stdout)]
                                     (send-translate-link chat-id link)
                                     {:link link :errors []})))
                                ((fn []
                                    (send-translate-link chat-id (clojure.string/join stderr))

                                   {:link "" :errors stderr}))))))

                        (catch Exception e
                          (error "PROCESS BUILDER ERROR" e))))]
    [process future-task]))
