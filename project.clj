(defproject meetup-users "0.1.0-SNAPSHOT"
  :description "Sample app for CLJ meetup in AKL."
  :url "https://github.com/fgeller/meetup-users"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [liberator "0.12.0"]
                 [compojure "1.1.3"]
                 [ring "1.2.1"]
                 [org.clojure/data.json "0.2.4"]
                 [com.datomic/datomic-free "0.9.4815.12"]]
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.5"]
                        [midje "1.6.3"]]}})
