(ns meetup-users.core
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [datomic.api :only [q db] :as peer]
   [liberator.core :refer [resource defresource]]
   [liberator.dev :refer [wrap-trace]]
   [compojure.core :refer [defroutes ANY]]
   [ring.adapter.jetty :refer [run-jetty]]))

(def datomic-uri "datomic:mem://users")
(def schema-tx [])
(defn setup-database []
  (peer/create-database datomic-uri)
  @(peer/transact (peer/connect datomic-uri) schema-tx))
(defn delete-database []
  (peer/delete-database datomic-uri))
(defn read-database []
  (peer/db (peer/connect datomic-uri)))

(setup-database)

(defresource users-resource)

(defroutes app-routes (ANY "/users" [] users-resource))

(def handlers (wrap-trace app-routes :header :ui))

(defn boot [port] (run-jetty #'handlers {:port port :join? false}))
