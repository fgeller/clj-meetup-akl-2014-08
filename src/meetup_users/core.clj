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
(defn read-database [] (peer/db (peer/connect datomic-uri)))
(def schema-tx [{:db/id #db/id[:db.part/db]
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
  @(peer/transact (peer/connect datomic-uri) schema-tx))
(defn delete-database []
  (peer/delete-database datomic-uri))

(setup-database)

(defn all-users [database]
  (into {}
        (map (fn [entity] {(:user/id entity) {:nick (:user/nick entity)}})
             (map (fn [[entity-id]] (peer/entity database entity-id))
                   (peer/q '[:find ?u :where [?u :user/id]] database)))))

(defn body-as-string [context]
  (if-let [body (get-in context [:request :body])]
    (condp instance? body
      java.lang.String body
      (slurp (io/reader body)))))

(defn parse-json [context]
  (when (#{:post} (get-in context [:request :request-method]))
    (if-let [body (body-as-string context)]
      (let [data (json/read-str body)]
        [false {::data data}])
      {:message "No body"})))

(defn add-user [database data]
  (let [new-id (+ 1 (count (peer/q '[:find ?u :where [?u :user/id]] database)))
        user-tx {:db/id (peer/tempid :db.part/user) :user/id new-id :user/nick (get data "nick")}]
    @(peer/transact (peer/connect datomic-uri) [user-tx])
    new-id))

(defresource users-resource
  :available-media-types ["application/json"]
  :allowed-methods [:get :post]
  :malformed? #(parse-json %)
  :post! (fn [context] (add-user (read-database) (::data context)))
  :handle-ok (fn [_] (all-users (read-database))))

(defroutes app-routes (ANY "/users" [] users-resource))

(def handlers (wrap-trace app-routes :header :ui))

(defn boot [port]
  (run-jetty #'handlers {:port port :join? false}))
