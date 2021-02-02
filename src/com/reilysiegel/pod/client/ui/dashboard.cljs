(ns com.reilysiegel.pod.client.ui.dashboard
  (:require [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [com.reilysiegel.pod.client.ui.material :as mui]
            [com.reilysiegel.pod.client.ui.person :as ui.person]
            [com.reilysiegel.pod.person :as person]))

(defsc Dashboard [_ {::person/keys [authed]
                     :as           props}]
  {:ident         (fn [] [:component/id ::dashboard])
   :query         [{::person/authed (comp/get-query ui.person/Person)}]
   :route-segment [""]
   :ui.menu/label "Dashboard"
   :ui.menu/order 0
   :ui.menu/icon  mui/dashboard-icon
   :initial-state {::person/authed {}}
   :will-enter
   (fn [app _]
     (dr/route-deferred
      [:component/id ::dashboard]
      #(df/load app
                ::person/authed
                ui.person/Person
                {:target               [:component/id ::dashboard ::person/authed]
                 :post-mutation        `dr/target-ready
                 :post-mutation-params {:target [:component/id ::dashboard]}})))}
  (prn props)
  (ui.person/ui-person authed))

(def ui-dashboard (comp/factory Dashboard))
