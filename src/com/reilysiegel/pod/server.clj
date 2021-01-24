(ns com.reilysiegel.pod.server
  (:require [com.reilysiegel.pod.server.database :as db]
            [com.reilysiegel.pod.server.handler :as handler]
            [com.reilysiegel.pod.server.parser :as parser]
            [com.reilysiegel.pod.utils :as utils]
            [datahike.api :as d]
            [integrant.core :as ig]
            [com.reilysiegel.pod.person :as person]
            [com.reilysiegel.pod.score :as score]
            [com.reilysiegel.pod.task :as task]
            [com.wsscode.pathom3.interface.eql :as p.eql]))

(def config
  {::db/conn        {:backend :file
                     :path    (str (System/getProperty "user.home") "/.pod/db")}
   ::parser/parser  {::db/conn (ig/ref ::db/conn)}
   ::handler/server {::db/conn       (ig/ref ::db/conn)
                     ::parser/parser (ig/ref ::parser/parser)
                     ::handler/port  3000}})

(def system (atom nil))

(defn init! []
  (reset! system (ig/init config)))

(defn halt! []
  (swap! system ig/halt!))

(comment
  (init!)
  (halt!)

  (->>   (d/q '[:find ?pid ?tx
                :keys com.reilysiegel.pod.person/id db/txInstant
                :where
                [?t ::task/id #uuid "600dd9af-135d-4234-8427-1173717b4b0c"]
                [?t ::task/person ?p ?tx]
                [?p ::person/id ?pid]]
              (d/history @(::db/conn @system)))
         (sort-by :db/txInstant)
         (map #(dissoc % :db/txInstant))
         dedupe)

  (d/datoms (d/history @(::db/conn @system)) :aevt ::task/person)

  (d/q
   '[:find ?pid .
     :in $ ?id
     :where
     [?t ::task/id ?id]
     [?t ::task/person ?p]
     [?p ::person/id ?pid]]
   @(::db/conn @system)
   #uuid "60072597-398f-43db-8347-651cdcf8f4f3")
  ((::parser/parser @system)
   {}
   [{[:com.reilysiegel.pod.person/id
      #uuid "ba0c3b54-157f-429f-a688-01f0d836e55f"]
     [#_#_#_:com.reilysiegel.pod.person/id
      :com.reilysiegel.pod.person/name
      :com.reilysiegel.pod.person/op?
      :com.reilysiegel.pod.person/tasks
      :com.reilysiegel.pod.score/alpha 
      :com.reilysiegel.pod.score/beta
      #_:com.reilysiegel.pod.score/mean]}])
  
  (d/entity   @(::db/conn @system)
              (d/q '[:find ?e .
                     :where [?e :com.reilysiegel.pod.task/id ?id]]
                   @(::db/conn @system)))
  
  (let [{:keys [db-before db-after]}
        (d/transact (::db/conn @system)
                    {:com.reilysiegel.pod.task/id (utils/squuid)
                     :effort                      13
                     :name                        "1"})]
    (= (d/datoms db-before :aevt) db-after ()))
  (d/datoms @(::db/conn @system) :aevt))
