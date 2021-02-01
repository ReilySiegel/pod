(ns com.reilysiegel.pod.server.database
  (:require [com.reilysiegel.pod.person :as person]
            [com.reilysiegel.pod.server.database.person :as db.person]
            [com.reilysiegel.pod.task :as task]
            [datahike.api :as d]
            [integrant.core :as ig]
            [provisdom.spectomic.core :as ds]))

(defn schema []
  [[::person/id {:db/unique :db.unique/identity}]
   [::person/email {:db/unique :db.unique/value}]
   ::person/name
   ::person/op?
   ::person/password-hash
   
   [::task/id {:db/unique :db.unique/identity}]
   ::task/name
   ::task/effort
   ::task/date
   ::task/complete?
   ::task/late?
   ::task/person])

(defn initial-tx []
  (flatten [(ds/datomic-schema (schema))
            (db.person/initial-tx)]))

(defmethod ig/init-key ::conn [_ opts]
  (when-not (d/database-exists? opts)
    (d/create-database opts))
  (let [conn (d/connect opts)]
    (d/transact conn (vec (initial-tx)))
    conn))
