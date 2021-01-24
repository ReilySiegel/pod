(ns com.reilysiegel.pod.client.ui.components
  "Reusable components."
  (:require [com.fulcrologic.fulcro.algorithms.denormalize :as fdenorm]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.components :as comp]
            [com.reilysiegel.pod.client.ui.material :as mui]
            [com.fulcrologic.fulcro.algorithms.merge :as merge]
            [clojure.spec.alpha :as s]))

(defn validating-form-field [{:keys [invalid? helper-text error-text]
                              :as   opts
                              :or   {helper-text " "
                                     error-text  " "}}]
  (mui/text-field (-> opts
                      (dissoc :invalid? helper-text error-text)
                      (merge 
                       {:error      invalid?
                        :helperText (if invalid? error-text helper-text)}))))

(defn form-submit [this
                   {:keys [onClick validators]
                    :as   opts}
                   label]
  (let [props         (comp/props this)
        ident         (comp/ident this props)
        state-map     (fs/mark-complete*
                       (merge/merge-component {} this props)
                       ident)
        updated-props (fdenorm/db->tree
                       (comp/query this)
                       (get-in state-map ident)
                       state-map)
        valid?        (and (fs/valid-spec? updated-props)
                           (every? #{:valid} (map #(% updated-props)
                                                  validators)))
        complete?     (fs/checked? props)]
    (mui/button
     (merge opts
            {:disabled (and complete? (not valid?))
             :onClick
             (fn [evt]
               (if valid?
                 (onClick evt)
                 (comp/transact! this [(fs/mark-complete!)])))})
     label)))
