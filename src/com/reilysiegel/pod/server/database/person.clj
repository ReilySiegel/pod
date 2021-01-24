(ns com.reilysiegel.pod.server.database.person
  (:require [buddy.hashers :as ph]
            [com.reilysiegel.pod.person :as person]
            [com.reilysiegel.pod.utils :as util]
            [com.wsscode.pathom3.connect.operation :as pco]
            [datahike.core :as d]
            [clojure.string :as str]))

(defn schema []
  [{:db/ident       ::person/name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       ::person/id
    :db/valueType   :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}
   {:db/ident       ::person/op?
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}
   {:db/ident       ::person/email
    :db/valueType   :db.type/string
    :db/unique      :db.unique/value
    :db/cardinality :db.cardinality/one}
   {:db/ident       ::person/password-hash
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

(defn initial-tx []
  [{::person/name          "Default Operator"
    ::person/email         "default@pod"
    ::person/op?           true
    ::person/id            #uuid "ba0c3b54-157f-429f-a688-01f0d836e55f"
    ::person/password-hash (ph/derive "password")}])

(pco/defresolver all-user-ids [{:com.reilysiegel.pod.server.database/keys
                                [conn]} _]
  {::pco/output [{::person/all [::person/id]}]}
  {::person/all (d/q '[:find ?id
                       :keys com.reilysiegel.pod.person/id
                       :where [_ ::person/id ?id]]
                     @conn)})

(defn resolvers []
  [all-user-ids
   (util/pull-resolver ::person/id 
                       [::person/name ::person/op? ::person/email])
   (util/pull-resolver ::person/email [::person/id])])
