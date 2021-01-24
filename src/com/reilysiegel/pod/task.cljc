(ns com.reilysiegel.pod.task
  (:require #?@(:clj [[datahike.api :as d]])
            #?@(:cljs [[com.fulcrologic.fulcro.algorithms.form-state :as fs]
                       [com.fulcrologic.fulcro.components :as comp]
                       [com.fulcrologic.fulcro.data-fetch :as df]
                       [com.fulcrologic.fulcro.mutations :as m]])
            #_[tick.alpha.api :as t]
            [clojure.spec.alpha :as s]
            [com.reilysiegel.pod.person :as person]
            [com.reilysiegel.pod.specs :as specs]
            [com.reilysiegel.pod.utils :as utils]
            [com.wsscode.pathom3.connect.operation :as pco]
            [com.reilysiegel.pod.server.database :as db]))

(s/def ::id uuid?)

(s/def ::name ::specs/non-empty-string)

(s/def ::effort pos-int?)

#_(s/def ::date t/date?)

#_(s/def ::time t/time?)

(s/def ::date inst?)

(s/def ::weekday
  #{:monday :tuesday :wednesday :thursday :friday :saturday :sunday})

(s/def ::important? boolean?)

(s/def ::recurring? boolean?)

(s/def ::complete? boolean?)

(s/def ::late? boolean?)

(s/def ::person ::person/person)

(s/def ::task (s/keys :req [::name ::id ::effort]
                      :opt [::date ::time ::weekday ::important? ::person
                            ::recurring?]))

#?(:clj
   (pco/defmutation upsert [{:com.reilysiegel.pod.server.database/keys [conn]}
                            {::keys        [id name effort time-inst date-inst]
                             ::person/keys [authed?]}]
     {::pco/global-input [::person/authed?]}
     (when authed?
       (let [task {::id     id
                   ::name   name
                   ::effort effort}]
         (when (s/valid? ::task task)
           (d/transact! conn [task])
           {::id id}))))
   :cljs
   (m/defmutation upsert [params]
     (remote [_] true)))

#?(:clj
   (pco/defmutation delete [{:com.reilysiegel.pod.server.database/keys [conn]}
                            {::keys                [id]
                             {::person/keys [op?]} ::person/authed}]
     {::pco/global-input [{::person/authed [::person/op?]}]}
     (when op?
       (d/transact! conn [[:db/retractEntity [::id id]]])
       {}))
   :cljs
   (m/defmutation delete [params]
     (remote [_] true)))

#?(:clj
   (pco/defmutation assign [{:com.reilysiegel.pod.server.database/keys [conn]}
                            {::keys        [id]
                             ::person/keys [authed?]
                             person-id     ::person/id}]
     {::pco/global-input [::person/authed?]}
     (when authed?
       (if person-id
         (d/transact! conn [{::id     id
                             ::person {::person/id person-id}}])
         (d/transact! conn [[:db/retract [::id id] ::person]]))
       {::id id}))
   :cljs
   (m/defmutation assign [params]
     (remote [env] (m/returning env
                                (comp/registry-key->class
                                 :com.reilysiegel.pod.client.ui.task/TaskCard)))))

#?(:clj
   (pco/defmutation complete [{:com.reilysiegel.pod.server.database/keys [conn]}
                              {::keys        [id]
                               ::person/keys [authed?]
                               :as           params
                               person-id     ::person/id}]
     {::pco/global-input [::person/authed?]}
     (when authed?
       (d/transact! conn [(select-keys params [::id ::late? ::complete?])])
       {::id id}))
   :cljs
   (m/defmutation complete [params]
     (remote [env] (m/returning env
                                (comp/registry-key->class
                                 :com.reilysiegel.pod.client.ui.task/TaskCard)))))




#?(:cljs
   (m/defmutation edit-new-task [_]
     (action [{:keys [state]}]
             (let [id    (utils/squuid)
                   ident [::id id]]
               (swap! state
                      (fn [s]
                        (-> s
                            (assoc-in ident {::id     id
                                             ::name   ""
                                             ::effort ""})
                            (assoc-in [:component/id ::create-task ::basic-task]
                                      ident)
                            (fs/add-form-config*
                             (comp/registry-key->class
                              :com.reilysiegel.pod.client.ui.task/BasicForm)
                             ident))))))))

#?(:clj
   (pco/defresolver all-incomplete
     [{:com.reilysiegel.pod.server.database/keys [conn]}
      _]
     {::pco/output [{::incomplete [::id]}]}
     {::incomplete
      (d/q '[:find ?id
             :keys com.reilysiegel.pod.task/id
             :where
             [?e ::id ?id]
             (or [?e ::late? false]
                 [(missing? $ ?e ::late?)])
             (or [?e ::complete? false]
                 [(missing? $ ?e ::complete?)])]
           @conn)}))

#?(:clj
   (pco/defresolver task->person
     [{:com.reilysiegel.pod.server.database/keys [conn]}
      {::keys [id]}]
     {::person/id
      (d/q '[:find ?pid .
             :in $ ?id
             :where
             [?t ::id ?id]
             [?t ::person ?p]
             [?p ::person/id ?pid]]
           @conn
           id)}))

#?(:clj
   (pco/defresolver task->people
     [{:com.reilysiegel.pod.server.database/keys [conn]}
      {::keys [id]}]
     {::pco/output [{::people [::person/id]}]}
     {::people
      (->> (d/q '[:find ?pid ?tx
                  :keys com.reilysiegel.pod.person/id db/txInstant
                  :in $ ?id
                  :where
                  [?t ::id ?id]
                  [?t ::person ?p ?tx]
                  [?p ::person/id ?pid]]
                (d/history @conn)
                id)
           (sort-by :db/txInstant)
           (map #(dissoc % :db/txInstant))
           dedupe
           vec)}))


#?(:clj
   (pco/defresolver person->tasks
     [{:com.reilysiegel.pod.server.database/keys [conn]}
      {::person/keys [id]}]
     {::pco/output [{::person/tasks [::id]}]}
     {::person/tasks
      (d/q '[:find ?tid
             :keys com.reilysiegel.pod.task/id
             :in $ ?id
             :where
             [?t ::id ?tid]
             [?t ::person ?p]
             [?p ::person/id ?id]]
           @conn
           id)}))

(pco/defresolver status-text [_ {::keys        [complete? late?]
                                 ::person/keys [id]
                                 :as           input}]
  {::pco/input [(pco/? ::complete?) (pco/? ::late?) (pco/? ::person/id)]}
  {::status-text (case [(some? id) (boolean complete?) (boolean late?)]
                   [true true false]  "Completed"
                   [true true true]   "Completed late"
                   [true false true]  "Incomplete"
                   [true false false] "Assigned"
                   "Available")})

#?(:clj (defn resolvers []
          [upsert
           assign
           complete
           delete
           task->person
           task->people
           person->tasks
           status-text
           all-incomplete
           (utils/pull-resolver ::id [::name ::effort ::complete? ::late?])]))
