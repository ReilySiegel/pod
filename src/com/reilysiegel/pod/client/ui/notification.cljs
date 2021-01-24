(ns com.reilysiegel.pod.client.ui.notification
  (:require [com.fulcrologic.fulcro.components :as comp]
            [com.fulcrologic.fulcro.mutations :as m]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.reilysiegel.pod.client.ui.material :as mui]))


(m/defmutation notify [{:keys [message severity]}]
  (action [{:keys [state]}]
          (swap!
           state
           (fn [s]
             (-> s
                 (assoc-in [:component/id ::notifier :ui/open?] true)
                 (assoc-in [:component/id ::notifier :ui/message] message)
                 (assoc-in [:component/id ::notifier :ui/severity] severity))))))

(comp/defsc Notifier [this {:ui/keys [message severity open?]}]
  {:ident         (fn [] [:component/id ::notifier])
   :query         [:ui/message :ui/severity :ui/open?]
   :initial-state {:ui/open? false}}
  (mui/snackbar
   {:open             open?
    :autoHideDuration 5000
    :onClose          #(m/toggle! this :ui/open?)}
   (mui/alert {:severity severity} message)))

(def ui-notifier (comp/factory Notifier))
