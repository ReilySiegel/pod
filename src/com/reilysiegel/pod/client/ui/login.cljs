(ns com.reilysiegel.pod.client.ui.login
  (:require [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.algorithms.merge :as merge]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.dom.events :as evt]
            [com.fulcrologic.fulcro.mutations :as m]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [com.fulcrologic.fulcro.ui-state-machines :as uism]
            [com.reilysiegel.pod.client.routing :as routing]
            [com.reilysiegel.pod.client.ui.components :as comps]
            [com.reilysiegel.pod.client.ui.material :as mui]
            [com.reilysiegel.pod.person :as person]
            [com.reilysiegel.pod.utils :as utils]))

(defsc LoginForm [this {:ui/keys [email password] :as props}]
  {:query         [:ui/email :ui/password [::uism/asm-id '_]]
   :ident         (fn [] [:component/id :login])
   :route-segment ["login"]
   :initial-state {:ui/email    ""
                   :ui/password ""}}
  (let [current-state    (uism/get-active-state
                          this
                          :com.reilysiegel.pod.client.session/sessions)
        busy?            (= :state/checking-credentials current-state)
        bad-credentials? (= :state/bad-credentials current-state)
        error?           (= :state/server-failed current-state)]
    (dom/div
     (mui/box
      {:sx {:m 1}}
      (mui/text-field {:value    email
                       :type     :email
                       :label    "Email"
                       :disabled busy?
                       :onChange #(m/set-string! this :ui/email :event %)}))
     (mui/box
      {:sx {:m 1}}
      (mui/text-field
       {:type     "password"
        :label    "Password"
        :value    password 
        :disabled busy?
        :onKeyDown
        (fn [evt]
          (when (evt/enter-key? evt)
            (uism/trigger!
             this
             :com.reilysiegel.pod.client.session/sessions
             :event/login
             {::person/email    email
              ::person/password password})))
        :onChange #(m/set-string! this :ui/password :event %)}))
     (mui/box
      {:sx {:m 1}}
      (mui/button 
       {:onClick (fn [e]
                   (uism/trigger! this
                                  :com.reilysiegel.pod.client.session/sessions
                                  :event/login
                                  {::person/email    email
                                   ::person/password password})
                   (m/set-string! this :ui/email :value "")
                   (m/set-string! this :ui/password :value ""))}
       "Login")))))

(def ui-login-form (comp/factory LoginForm))

(m/defmutation signup [params]
  (remote [env] (m/with-server-side-mutation env
                  'com.reilysiegel.pod.server.database.session/signup))
  (ok-action [{:keys [app result]}]
             (comp/transact! app [(routing/route-to {:route ["login"]})])))

(def signup-password-validator
  (fs/make-validator (fn valid
                       ([form] (valid form  nil))
                       ([{::person/keys [password password2]} _]
                        (= password password2)))))

(defsc SignupForm [this {::person/keys [id name email password password2]
                         :as           props}]
  {:query         [::person/id ::person/name ::person/email ::person/password
                   ::person/password2
                   fs/form-config-join]
   :ident         ::person/id
   :form-fields   #{::person/id ::person/name ::person/email ::person/password
                    ::person/password2}
   :pre-merge     (fn [{:keys [data-tree]}]
                    (fs/add-form-config SignupForm data-tree))
   :route-segment ["signup"]
   :will-enter
   (fn [app _params]
     (let [{::person/keys [id] :as person}
           (comp/get-initial-state SignupForm)]
       (dr/route-deferred [::person/id id]
                          (fn []
                            (merge/merge-component! app SignupForm person)
                            (dr/target-ready! app [::person/id id])))))
   :initial-state (fn [_]
                    {::person/id        (utils/squuid)
                     ::person/name      ""
                     ::person/email     ""
                     ::person/password  ""
                     ::person/password2 ""})}
  (let [passwords-invalid? (= :invalid
                              (signup-password-validator props
                                                         ::person/password2))]
    (comp/fragment
     (mui/box
      {:sx {:m 1}}
      (comps/validating-form-field
       {:value    name
        :label    "Name"
        :invalid? (fs/invalid-spec? props ::person/name)
        :onChange #(m/set-string! this ::person/name :event %)
        :onBlur   #(comp/transact! this [(fs/mark-complete!
                                          {:field ::person/name})])}))
     (mui/box
      {:sx {:m 1}}
      (comps/validating-form-field
       {:value      email
        :type       :email
        :label      "Email"
        :invalid?   (fs/invalid-spec? props ::person/email)
        :error-text "Please enter a valid email address."
        :onChange   #(m/set-string! this ::person/email :event %)
        :onBlur     #(comp/transact! this [(fs/mark-complete!
                                            {:field ::person/email})])}))
     (mui/box
      {:sx {:m 1}}
      (comps/validating-form-field
       {:type       "password"
        :label      "Password"
        :invalid?   (fs/invalid-spec? props ::person/password)
        :error-text "Please enter a valid password."
        :value      password 
        :onChange   #(m/set-string! this ::person/password :event %)
        :onBlur     #(comp/transact! this [(fs/mark-complete!
                                            {:field ::person/password})])}))
     (mui/box
      {:sx {:m 1}}
      (comps/validating-form-field
       {:type       "password"
        :label      "Confirm Password"
        :invalid?   passwords-invalid?
        :value      password2
        :error-text "Passwords must match."
        :onChange   #(m/set-string! this ::person/password2 :event %)
        :onBlur     #(comp/transact! this [(fs/mark-complete!
                                            {:field ::person/password2})])}))
     (mui/box
      {:sx {:m 1}}
      (comps/form-submit
       this
       {:validators [signup-password-validator]
        :onClick    #(comp/transact! this
                                     [(signup #::person{:id       id
                                                        :name     name
                                                        :password password
                                                        :email    email})])}
       "Signup")))))

(def ui-signup-form (comp/factory SignupForm))
