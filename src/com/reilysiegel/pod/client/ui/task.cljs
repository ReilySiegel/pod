(ns com.reilysiegel.pod.client.ui.task
  (:require [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.components :as comp]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.mutations :as m]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [com.reilysiegel.pod.client.session :as session]
            [com.reilysiegel.pod.client.ui.components :as comps]
            [com.reilysiegel.pod.client.ui.material :as mui]
            [com.reilysiegel.pod.task :as task]
            [com.reilysiegel.pod.utils :as utils]
            [com.reilysiegel.pod.person :as person]))

(comp/defsc BasicForm [this
                       {::task/keys [id name effort]
                        :as         props}
                       {:keys [complete]}]
  {:ident         ::task/id
   :query         [::task/id ::task/name ::task/effort
                   fs/form-config-join]
   :form-fields   #{::task/id ::task/name ::task/effort}
   :initial-state {}}
  (comp/fragment
   (mui/dialog-content
    {}
    (mui/box
     {:sx {:p 1}}
     (mui/grid
      {:container true
       :spacing   4}
      (mui/grid {:item true
                 :xs   12
                 :sm   6}
                (comps/validating-form-field
                 {:value     name
                  :fullWidth true
                  :autoFocus true
                  :invalid?  (fs/invalid-spec? props ::task/name)
                  :onChange  #(m/set-string! this ::task/name :event %)
                  :label     "Name"}))
      (mui/grid {:item true
                 :xs   12
                 :sm   6}
                (comps/validating-form-field
                 {:value      (str effort)
                  :type       :number
                  :fullWidth  true
                  :invalid?   (fs/invalid-spec? props ::task/effort)
                  :error-text "Effort should be a positive integer"
                  :onChange   #(m/set-integer! this ::task/effort :event %)
                  :label      "Effort"})))))
   (mui/dialog-actions
    {}
    (comps/form-submit
     this
     {:sx      {:order 1}
      :onClick (fn [_]
                 (comp/transact! this [(task/upsert
                                        (select-keys
                                         props
                                         (fs/get-form-fields this)))])
                 (when complete
                   (complete)))}
     "Add Task")
    (mui/button {:sx      {:order 0}
                 :color   :secondary
                 :onClick (fn []
                            (comp/transact! this [(fs/reset-form!)])
                            (when complete
                              (complete)))}
                "Cancel"))))

(def ui-basic-form (comp/computed-factory BasicForm))

(comp/defsc Create [this {:ui/keys    [open? tab]
                          ::task/keys [basic-task]
                          :as         props}]
  {:query         [[::session/session '_]
                   :ui/open? :ui/tab
                   {::task/basic-task (comp/get-query BasicForm)}]
   :ident         (fn [_] [:component/id ::task/create-task])
   :initial-state {:ui/open?         false
                   ::task/basic-task {}
                   :ui/tab           ::basic-form}
   :use-hooks?    true}
  (let [{:keys [spacing]} (mui/use-theme)
        fullscreen?       (mui/use-breakpoint :sm :down)]
    (comp/fragment
     (mui/dialog
      {:fullScreen fullscreen?
       :fullWidth  true
       :open       open?
       :onClose    #(m/set-value! this :ui/open? false)}
      (mui/tab-context
       {:value tab}
       (mui/tab-list
        {:variant  :fullWidth
         :onChange #(m/set-value! this :ui/tab %2)}
        (mui/tab {:label "Basic Task" :value ::basic-form})
        (mui/tab {:label "Recurring Task" :value ::recurring-form :disabled true}))
       (mui/tab-panel
        {:value ::basic-form}
        (ui-basic-form basic-task
                       {:complete #(m/set-value! this :ui/open? false)}))
       (mui/tab-panel {:value ::recurring-form} "1")))
     (when (utils/authed? props)
       (mui/fab {:color     :primary
                 :sx        {:position :absolute
                             :bottom   (spacing 2)
                             :right    (spacing 2)}
                 :autoFocus true
                 :onClick   #(comp/transact! this [(task/edit-new-task)
                                                   (m/toggle {:field :ui/open?})])}
                (mui/add-icon))))))

(def ui-create (comp/factory Create))

(comp/defsc Person [this {::person/keys [name]}]
  {:ident ::person/id
   :query [::person/id ::person/name]}
  (mui/typography {:variant :subtitle1 :color :textSecondary :noWrap true} name))

(def ui-person (comp/factory Person #_{:keyfn ::person/id}))

(comp/defsc TaskCard [this {::task/keys   [id name effort complete? late? status-text
                                           people]
                            ::person/keys [authed]
                            :ui/keys      [open?]
                            person-id     ::person/id
                            person-name   ::person/name
                            :as           props}]
  {:ident         ::task/id
   :query         [::task/id ::task/name ::task/effort ::task/complete?
                   ::task/late? ::person/name ::task/status-text ::person/id
                   :ui/open?
                   {::task/people (comp/get-query Person)}
                   {::person/authed [::person/id ::person/name ::person/op?]}]
   :initial-state {:ui/open? false}
   :use-hooks?    true}
  (let [{authed-id ::person/id
         op?       ::person/op?} authed
        {{:keys [create duration]}
         :transitions}           (mui/use-theme)]
    (mui/grid
     {:item true
      :xs   12
      :sm   6
      :md   4
      :lg   3}
     (mui/card
      {#_#_:onClick
       #(comp/transact! this [(routing/route-to {:route ["task" id]})])}
      (mui/card-content
       {}
       (mui/typography {:variant :h5 :component :h6} name)
       (mui/typography {:variant :subtitle1 :color :textSecondary}
                       (str "Effort: " effort " Minutes"))
       
       (mui/typography {:variant :subtitle1 :color :textSecondary :noWrap true}
                       status-text)
       (mui/typography {:variant :subtitle1 :color :textSecondary :noWrap true}
                       person-name)
       (when op?
         (comp/fragment
          (mui/form-control-label
           {:label    "Complete"
            :disabled (not person-id)
            :control  (mui/checkbox
                       {:checked (boolean complete?)
                        :onClick #(comp/transact!
                                   this
                                   [(task/complete
                                     #::task{:id        id
                                             :complete? (not complete?)})])})})
          (mui/form-control-label
           {:label    "Late"
            :disabled (not person-id)
            :control  (mui/checkbox
                       {:checked (boolean late?)
                        :onClick #(comp/transact!
                                   this
                                   [(task/complete
                                     #::task{:id    id
                                             :late? (not late?)})])})}))))
      (mui/card-actions
       {}
       (if-not (= person-id authed-id)
         (mui/tooltip
          {:title "Claim"}
          (mui/icon-button
           {:onClick #(comp/transact!
                       this
                       [(task/assign {::task/id   id
                                      ::person/id authed-id})])}
           (mui/assignment-ind-icon)))
         (when (and (= authed-id person-id)
                    (not complete?))
           (mui/tooltip
            {:title "Return"}
            (mui/icon-button
             {:onClick #(comp/transact!
                         this
                         [(task/assign {::task/id id})])}
             (mui/assignment-return-icon)))))
       (when op?
         (comp/fragment
          (mui/tooltip
           {:title "Assign"}
           (mui/icon-button
            {:onClick #(comp/transact!
                        this
                        [(task/assign {::task/id   id
                                       ::person/id true})])}
            (mui/assignment-returned-icon)))
          (mui/tooltip
           {:title "Delete"}
           (mui/icon-button
            {:onClick #(comp/transact!
                        this
                        [(task/delete {::task/id id})])}
            (mui/delete-icon)))))
       (mui/tooltip
        {:title "History"}
        (mui/icon-button
         {:sx      {:marginLeft "auto !important"
                    :transform  (if open? "rotate(180deg)" "rotate(0deg)")
                    :transition (str (create "transform" #js {:duration (:shortest duration)})
                                     " !important")}
          :onClick #(m/toggle! this :ui/open?)}
         (mui/expand-more-icon))))
      (mui/collapse
       {:in open? :timeout :auto :unmountOnExit true}
       (mui/card-content
        {}
        (map ui-person (reverse people))))))))

(def ui-task-card (comp/factory TaskCard {:keyfn ::task/id}))

(comp/defsc TaskList [this {::task/keys [incomplete]
                            ::keys      [create]}]
  {:ident         (fn [] [:component/id ::task-list])
   :query         [{::task/incomplete (comp/get-query TaskCard)}
                   {::create (comp/get-query Create)}]
   :initial-state {::task/incomplete []
                   ::create          {}}
   :route-segment ["tasks"]
   :ui.menu/order 3
   :ui.menu/label "Tasks"
   :ui.menu/icon  mui/people-icon
   :will-enter
   (fn [app _params]
     (dr/route-deferred
      [:component/id ::task-list]
      (fn []
        (df/load!
         app
         ::task/incomplete
         TaskCard
         {:post-mutation        `dr/target-ready
          :post-mutation-params {:target [:component/id ::task-list]}
          :target               [:component/id ::task-list ::task/incomplete]}))))}
  (dom/div
   {:style {:width "100%"}}
   (mui/grid
    {:container true
     :spacing   3
     :sx        {:flexGrow 1}}
    (map-indexed (fn [idx props] (ui-task-card (assoc props :react-key (str idx)))) incomplete))
   (ui-create create)))
