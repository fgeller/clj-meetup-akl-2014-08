(ns meetup-users.core-test
  (:use meetup-users.core
        midje.sweet
        ring.mock.request)
  (:require [clojure.test :refer :all]))


(defn cleanup []
  (delete-database)
  (setup-database))

(fact "listing users"
      (let [response (handlers (request :get "/users"))]
        (:status response) => 200
        (:body response) => "{}")
      (cleanup))

(fact "adding and listing a user"
      (let [request (body (content-type (request :post "/users") "application/json") "{\"nick\": \"hans\"}")
            response (handlers request)]
        (:status response) => 201)

      (let [response (handlers (request :get "/users"))]
        (:status response) => 200
        (:body response) => "{\"1\":{\"nick\":\"hans\"}}")

      (cleanup))
