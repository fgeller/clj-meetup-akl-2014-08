(ns meetup-users.core
  (:require [liberator.core :refer [resource defresource]]
            [liberator.dev :refer [wrap-trace]]
            [compojure.core :refer [defroutes ANY]]
            [datomic.api :only [q db] :as peer]))

(defresource users-resource)

(defroutes app-routes (ANY "/users" [] users-resource))

(def handlers (wrap-trace app-routes :header :ui))
