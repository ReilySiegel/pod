(ns com.reilysiegel.pod.client.ui.person
  (:require [com.fulcrologic.fulcro.components :as comp]
            [com.reilysiegel.pod.person :as person]
            [com.fulcrologic.fulcro.dom :as dom]
            [clojure.string :as str]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [com.reilysiegel.pod.client.ui.material :as mui]
            [com.reilysiegel.pod.client.routing :as routing]
            [com.reilysiegel.pod.client.ui.score :as ui.score]
            [com.reilysiegel.pod.score :as score]
            [com.reilysiegel.pod.client.ui.task :as ui.task]))

(comp/defsc Person [_this {::person/keys [name op? tasks]
                           ::score/keys  [alpha beta mean]}]
  {:ident         ::person/id
   :query         [::person/id ::person/name ::person/op?
                   ::score/mean ::score/alpha ::score/beta
                   {::person/tasks (comp/get-query ui.task/TaskCard)}]
   :initial-state {}
   :route-segment ["person" ::person/id]
   :will-enter
   (fn [app {::person/keys [id]}]
     (when-let [id (some-> id uuid)]
       (dr/route-deferred
        [::person/id id]
        #(df/load app
                  [::person/id id]
                  Person
                  {:post-mutation        `dr/target-ready
                   :post-mutation-params {:target [::person/id id]}}))))}
  (mui/grid {:container true
             :spacing   2}
            (map ui.task/ui-task-card tasks)))

(def ui-person (comp/factory Person))

(comp/defsc PersonCard [this {::person/keys [id name email]
                              :>/keys       [score-graph]}]
  {:ident         ::person/id
   :query         [::person/id ::person/name ::person/email
                   {:>/score-graph (comp/get-query ui.score/ScoreGraph)}]
   :initial-state {}}
  (mui/grid
   {:item true
    :xs   12
    :sm   6
    :md   4
    :lg   3}
   (mui/card
    {:onClick
     #(comp/transact! this
                      [(routing/route-to {:route ["person" id]})])}
    (mui/card-content
     {}
     (ui.score/ui-score-graph score-graph)
     (mui/typography {:variant :h5 :component :h6} name)
     (mui/typography {:variant :subtitle1 :color :textSecondary} email)))))

(def ui-person-card (comp/factory PersonCard {:keyfn ::person/id}))

(comp/defsc PersonList [this {::person/keys [all]}]
  {:ident         (fn [] [:component/id ::person-list])
   :query         [{::person/all (comp/get-query PersonCard)
                    }]
   :initial-state {::person/all []}
   :route-segment ["people"]
   :ui.menu/order 2
   :ui.menu/label "People"
   :ui.menu/icon  mui/people-icon
   :will-enter
   (fn [app _params]
     (dr/route-deferred
      [:component/id ::person-list]
      (fn []
        (df/load!
         app
         ::person/all
         PersonCard
         {:post-mutation        `dr/target-ready
          :post-mutation-params {:target [:component/id ::person-list]}
          :target               [:component/id ::person-list ::person/all]}))))}
  (dom/div
   {:style {:width "100%"}}
   (mui/grid
    {:container true
     :spacing   3
     :sx        {:flexGrow 1}}
    (mapv ui-person-card all))))

(def person-list-ui (comp/factory PersonList))
