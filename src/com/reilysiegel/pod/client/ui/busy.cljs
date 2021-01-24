(ns com.reilysiegel.pod.client.ui.busy
  (:require [com.fulcrologic.fulcro.components :as comp]
            [com.fulcrologic.fulcro.application :as app]
            [com.reilysiegel.pod.client.ui.material :as mui]))

(comp/defsc Busy [_this {::app/keys [active-remotes]}]
  {:query         [[::app/active-remotes '_]]
   :ident         (fn [] [:component/id ::busy])
   :initial-state {}}
  (let [busy? (boolean (seq active-remotes))]
    (mui/box
     {:sx {:display (if busy? :block :none)}}
     (mui/linear-progress {:variant (if busy?
                                      :indeterminate
                                      :determinate)
                           :value   100
                           :color   :secondary}))))

(def ui-busy (comp/factory Busy))
