#_(ns meetup-users.core
    (:require
     [clojure.data.json :as json]
     [clojure.java.io :as io]
     [datomic.api :only [q db] :as peer]
     [liberator.core :refer [resource defresource]]
     [liberator.dev :refer [wrap-trace]]
     [compojure.core :refer [defroutes ANY]]
     [ring.adapter.jetty :refer [run-jetty]]))

#_(def datomic-uri "datomic:mem://users")
#_(def schema-tx [{:db/id #db/id[:db.part/db]
                   :db/ident :user/id
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one
                   :db.install/_attribute :db.part/db},
                  {:db/id #db/id[:db.part/db]
                   :db/ident :user/nick
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db.install/_attribute :db.part/db}])
#_(defn setup-database []
    (peer/create-database datomic-uri)
    @(peer/transact (peer/connect datomic-uri) schema-tx))
#_(defn delete-database []
    (peer/delete-database datomic-uri))
#_(defn read-database []
    (peer/db (peer/connect datomic-uri)))

#_(setup-database)


#_(defn find-all-users [database]
    (peer/q '[:find ?u :where [?u :user/id]] database))

#_(defn all-users [database]
    (into {}
          (map (fn [[entity-id]]
                 (let [entity (peer/entity database entity-id)]
                   {(:user/id entity) {:nick (:user/nick entity)}}))
               (find-all-users database))))

#_(defn add-user [database data]
    (let [new-id (+ 1 (count (find-all-users database)))
          user-tx {:db/id (peer/tempid :db.part/user) :user/id new-id :user/nick (get data "nick")}]
      @(peer/transact (peer/connect datomic-uri) [user-tx])
      new-id))

#_(defresource users-resource
    :available-media-types ["application/json"]
    :allowed-methods [:get :post]
    :malformed? (fn [context]
                  (when (= :post (get-in context [:request :request-method]))
                    (let [body (slurp (io/reader (get-in context [:request :body])))]
                      [false {::data (json/read-str body)}])))
    :post! (fn [context]
             (add-user (read-database) (::data context)))
    :handle-ok (fn [_] (all-users (read-database))))

#_(defroutes app-routes (ANY "/users" [] users-resource))

#_(def handlers (wrap-trace app-routes :header :ui))

#_(defn boot [port] (run-jetty #'handlers {:port port :join? false}))