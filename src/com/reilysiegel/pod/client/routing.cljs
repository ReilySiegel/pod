(ns com.reilysiegel.pod.client.routing
  (:require [clojure.string :as str]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [com.reilysiegel.pod.client.app :refer [SPA]]
            [pushy.core :as pushy]))

(defonce history
  (pushy/pushy
   (fn [p]
     (let [route-segments-raw (vec (rest (str/split p "/")))
           route-segments     (if (empty? route-segments-raw)
                                [""]
                                route-segments-raw)]
       (dr/change-route SPA route-segments)))
   identity))

(defn start! []
  (pushy/start! history))

(defn route-to!
  "Change routes to the given route-string (e.g. \"/home\"."
  [route-string]
  (pushy/set-token! history route-string))

(defmutation route-to
  [{:keys [route]}]
  (action [_]
          (route-to! (str \/ (str/join \/ route)))))
