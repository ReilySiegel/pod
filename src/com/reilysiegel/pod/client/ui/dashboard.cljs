(ns com.reilysiegel.pod.client.ui.dashboard
  (:require [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.reilysiegel.pod.client.ui.material :as mui]))

(defsc Dashboard [_ _]
  {:ident         (fn [] [:component/id ::dashboard])
   :query         []
   :route-segment [""]
   :ui.menu/label "Dashboard"
   :ui.menu/order 0
   :ui.menu/icon  mui/dashboard-icon
   :initial-state {}}
  "Dashboard")

(def ui-dashboard (comp/factory Dashboard))
