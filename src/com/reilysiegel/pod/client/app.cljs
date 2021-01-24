(ns com.reilysiegel.pod.client.app
  (:require 
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.networking.http-remote :as net]
   [com.fulcrologic.fulcro.components :as comp]))

(def secured-request-middleware
  (->
   (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
   (net/wrap-fulcro-request)))

(defonce SPA
  (app/fulcro-app
   {:remotes {:remote (net/fulcro-http-remote
                       {:url                "/api"
                        :request-middleware secured-request-middleware})}
    :global-error-action
    (fn [{:keys [app]}]
      (comp/transact! app ['(com.reilysiegel.pod.client.ui.notification/notify
                             {:message  "Server has encountered an error."
                              :severity :error})]))}))


