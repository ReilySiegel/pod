(ns com.reilysiegel.pod.server.database
  (:require 
   [com.reilysiegel.pod.database :as db]
   [com.reilysiegel.pod.person :as person]
   [com.reilysiegel.pod.task :as task]
   [datahike.api :as d]
   [integrant.core :as ig]
   [buddy.hashers :as ph]
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
            {::person/name          "Default Operator"
             ::person/email         "default@pod"
             ::person/op?           true
             ::person/id            #uuid "ba0c3b54-157f-429f-a688-01f0d836e55f"
             ::person/password-hash (ph/derive "password")}]))

(defmethod ig/init-key ::db/conn [_ opts]
  (when-not (d/database-exists? opts)
    (d/create-database opts))
  (let [conn (d/connect opts)]
    (d/transact conn (vec (initial-tx)))
    conn))
