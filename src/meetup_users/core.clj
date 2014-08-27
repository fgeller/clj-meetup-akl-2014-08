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
(def schema-txs [{:db/id #db/id[:db.part/db]
                  :db/ident :user/id
                  :db/valueType :db.type/long
                  :db/cardinality :db.cardinality/one
                  :db.install/_attribute :db.part/db},
                 {:db/id #db/id[:db.part/db]
                  :db/ident :user/nick
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db.install/_attribute :db.part/db}])
(defn setup-database []
  (peer/create-database datomic-uri)
  @(peer/transact (peer/connect datomic-uri) schema-txs))
(defn delete-database []
  (peer/delete-database datomic-uri))
(defn read-database []
  (peer/db (peer/connect datomic-uri)))

(setup-database)

(defn find-all-users [database]
  (peer/q '[:find ?u :where [?u :user/id ?i]] database))

(defn all-users [database]
  (into {}
        (map (fn [[entity-id]]
               (let [entity (peer/entity database entity-id)]
                 {(:user/id entity) {:nick (:user/nick entity)}}))
             (find-all-users database))))

(defn add-user [database data]
  (let [new-id (+ 1 (count (find-all-users database)))
        user-tx {:db/id (peer/tempid :db.part/user) :user/nick (get data "nick") :user/id new-id}]
    (peer/transact (peer/connect datomic-uri) [user-tx])))

(defresource users-resource
  :available-media-types ["application/json"]
  :handle-exception (fn [context] (println "EX:" (:exception context)))
  :allowed-methods [:get :post]
  :post! (fn [context]
           (let [body (slurp (io/reader (get-in context [:request :body])))
                 body-json (json/read-str body)]
             (add-user (read-database) body-json)))
  :handle-ok (fn [context]
               (all-users (read-database))))

(defroutes app-routes (ANY "/users" [] users-resource))

(def handlers (wrap-trace app-routes :header :ui))

(defn boot [port] (run-jetty #'handlers {:port port :join? false}))
