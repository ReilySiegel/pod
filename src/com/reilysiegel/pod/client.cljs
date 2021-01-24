(ns com.reilysiegel.pod.client
  (:require [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp]
            [com.fulcrologic.fulcro.ui-state-machines :as uism]
            [com.reilysiegel.pod.client.app :refer [SPA]]
            [com.reilysiegel.pod.client.routing :as routing]
            [com.reilysiegel.pod.client.session :as session]
            [com.reilysiegel.pod.client.ui.login :as login]
            [com.reilysiegel.pod.client.ui.root :as root]))

(defn refresh! []
  (app/mount! SPA root/Root "app" {:initialize-state? false})
  (comp/refresh-dynamic-queries! SPA))

(defn init! []
  (app/set-root! SPA root/Root {:initialize-state? true})
  (routing/start!)
  (uism/begin! SPA session/session-machine ::session/sessions
               {:actor/user       session/CurrentUser
                :actor/login-form login/LoginForm}
               {:desired-path (some-> js/window .-location .-pathname)})
  (app/mount! SPA root/Root "app" {:initialize-state? false}))


