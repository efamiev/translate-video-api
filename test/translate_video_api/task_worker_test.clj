(ns translate-video-api.task-worker-test
  (:require [clojure.test :refer [deftest testing is]] [translate-video-api.task-worker :as task-worker] [clojure.tools.logging :refer [info]]))

(deftest test-task-worker
  (testing "Close and remove the thread from the queue if it has finished and has been running for more than 10 minutes."
    (let [task-queue (atom [])
          manager-control (atom "WORK")
          task1 {:id 1 :task (future "ready") :start-time (System/currentTimeMillis)}
          task2 {:id 2 :task (future (Thread/sleep 5000)) :start-time (- (System/currentTimeMillis) 10000000)}
          task3 {:id 3 :task (future (Thread/sleep 3000)) :start-time (- (System/currentTimeMillis) 10000000)}]

      (reset! task-queue [task1 task2 task3])

      (task-worker/clean-tasks task-queue manager-control)

      (Thread/sleep 100)

      (info "QUEUE" @task-queue)

      (is (empty? @task-queue))
      (is (future-done? (:task task1)))
      (is (future-cancelled? (:task task2)))
      (is (future-cancelled? (:task task3))))))
