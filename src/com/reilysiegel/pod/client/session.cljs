(ns com.reilysiegel.pod.client.session
  (:require [com.fulcrologic.fulcro.algorithms.data-targeting :as dt]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.mutations :as m]
            [com.fulcrologic.fulcro.ui-state-machines
             :as
             uism
             :refer
             [defstatemachine]]
            [com.reilysiegel.pod.client.routing :as routing]
            [com.reilysiegel.pod.client.ui.material :as mui]
            [com.reilysiegel.pod.person :as person]))

(comp/defsc SessionQuery [_ _]
  {:query [::person/id]
   :ident ::person/id})

(defn handle-login [{::uism/keys [event-data] :as env}]
  (let [user-class (uism/actor-class env :actor/user)]
    (-> env
        (uism/trigger-remote-mutation
         :actor/login-form `com.reilysiegel.pod.server.database.session/login
         (merge event-data
                {::m/returning      user-class
                 ::dt/target        [:session/current-user]
                 ::uism/ok-event    :event/ok
                 ::uism/error-event :event/error}))
        (uism/activate :state/checking-credentials))))

(def main-events
  {:event/logout
   {::uism/handler
    (fn [env]
      (routing/route-to! "/login")
      (-> env
          (uism/trigger-remote-mutation
           :actor/login `com.reilysiegel.pod.server.database.session/logout {})
          (uism/apply-action assoc-in [::session :current-user]
                             {::person/id nil})))}
   :event/login {::uism/handler handle-login}})

(defstatemachine session-machine
  {::uism/actor-name
   #{:actor/user
     :actor/login-form}

   ::uism/aliases
   {:logged-in? [:actor/user ::person/id]}

   ::uism/states
   {:initial
    {::uism/handler
     (fn [{::uism/keys [event-data] :as env}]
       (-> env
           (uism/store :config event-data)
           (uism/load ::person/authed :actor/user
                      {::uism/ok-event    :event/ok
                       ::uism/error-event :event/error})
           (uism/activate :state/checking-existing-session)))}
    :state/checking-existing-session
    {::uism/events
     {:event/ok    {::uism/handler
                    (fn [env]
                      (let [logged-in? (uism/alias-value env :logged-in?)]
                        (when-not logged-in?
                          (routing/route-to! "/login"))
                        (uism/activate env :state/idle)))}
      :event/error {::uism/handler
                    (fn [env]
                      (uism/activate env :state/server-failed))}}}

    :state/bad-credentials
    {::uism/events main-events}

    :state/idle
    {::uism/events main-events}

    :state/checking-credentials
    {::uism/events
     {:event/ok
      {::uism/handler
       (fn [env]
         (let [logged-in?             (uism/alias-value env :logged-in?)
               {:keys [desired-path]} (uism/retrieve env :config)]
           (when (and logged-in? desired-path)
             (routing/route-to! desired-path))
           (-> env
               (uism/activate (if logged-in?
                                :state/idle
                                :state/bad-credentials)))))}
      :event/error
      {::uism/handler
       (fn [env]
         (uism/activate env :state/server-failed))}}}

    :state/server-failed
    {::uism/events main-events}}})

(defsc CurrentUser [this {:keys [::person/id] :as props}]
  {:query         [::person/id]
   :initial-state {::person/id nil}
   :ident         (fn [] [::session :current-user])}
  (if id
    (mui/button {:color :inherit
                 :onClick
                 (fn [] (uism/trigger! this ::sessions :event/logout))}
                "Logout")
    (comp/fragment
     (mui/button
      {:color   :inherit
       :onClick #(comp/transact! this [(routing/route-to {:route
                                                          ["signup"]})])}
      "Signup")
     (mui/button
      {:color   :inherit
       :onClick #(comp/transact! this [(routing/route-to {:route
                                                          ["login"]})])}
      "Login"))))

(def ui-current-user (comp/factory CurrentUser))
