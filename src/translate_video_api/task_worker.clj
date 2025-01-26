(ns translate-video-api.task-worker
  (:require [clojure.core.async :refer [chan go go-loop >! >!! <! <!! close! timeout]]
            [clojure.tools.logging :refer [info error]]))

(def worker-timeout (* 5 60 1000))

(defn add-task [task-queue task]
  (swap! task-queue conj task))

(defn clean-tasks [task-queue manager-control]
  ;TODO:Add manager-control handling to enable and disable the loop
  (go-loop []
    (let [now (System/currentTimeMillis)]
      (swap!
       task-queue
       (fn [futures]
         (reduce (fn [acc el]
                   (cond
                     (future-done? (:task el)) acc
                     (< (- now (:start-time el)) worker-timeout) (conj acc el)
                     :else (try
                             (future-cancel (:task el))
                             acc
                             (catch Exception e (info "CANCEL FUTURE ERROR" el e)))))
                 []
                 futures))))

    (timeout worker-timeout)
    (recur)))
