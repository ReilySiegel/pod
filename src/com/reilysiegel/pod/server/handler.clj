(ns com.reilysiegel.pod.server.handler
  (:require [com.fulcrologic.fulcro.server.api-middleware :as fm]
            [hiccup.page :as hiccup]
            [integrant.core :as ig]
            [org.httpkit.server :as http]
            [ring.middleware.defaults :as rmd]
            [ring.util.response :as resp]))
            [ring.middleware.session.memory :as mem]

(defonce sessions (atom {}))

(defn index [csrf-token]
  (hiccup/html5
   [:html {:lang "en"}
    [:head {:lang "en"}
     [:title "Pod"]
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"}]
     [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css?family=Roboto:300,400,500,700&display=swap"}]
     [:link {:rel "shortcut icon" :href "data:image/x-icon;," :type "image/x-icon"}]
     [:script (str "var fulcro_network_csrf_token = '" csrf-token "';")]]
    [:body {:style "background-color: #2E3440"}
     [:div#app]
     [:script {:src "/js/main.js"}]]]))


(defn index-handler [{:keys [uri anti-forgery-token] :as req}]
  (-> (resp/response (index anti-forgery-token))
      (resp/content-type "text/html")))

(defn- wrap-api [handler uri parser]
  (fn [req]
    (if (= uri (:uri req))
      (fm/generate-response
       (let [parse-result
             (try
               (parser {:ring/request req} (:transit-params req))
               (catch Exception e e))]
         (if (instance? Throwable parse-result)
           {:status 500
            :body   {:error (ex-message parse-result)}}
           (merge {:status 200
                   :body   parse-result}
                  (fm/apply-response-augmentations parse-result)))))
      (handler req))))

(defmethod ig/init-key ::server [_ {:com.reilysiegel.pod.server.parser/keys
                                    [parser]
                                    ::keys [port]}]
  (http/run-server (-> index-handler
                       (wrap-api "/api" parser)
                       (fm/wrap-transit-params)
                       (fm/wrap-transit-response)
                       (rmd/wrap-defaults {:static  {:resources "public"}
                                           :session {:store (mem/memory-store sessions)}}))
                   {:port port}))

(defmethod ig/halt-key! ::server [_ server]
  (server))
