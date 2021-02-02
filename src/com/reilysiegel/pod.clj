(ns com.reilysiegel.pod
  (:gen-class)
  (:require [com.reilysiegel.pod.server :as server]
            [shadow.cljs.devtools.api :as api]
            [integrant.core :as ig]))

(defn compile-cljs
  {:shadow/requires-server true}
  [_opts]
  (api/release :frontend))

(defn start
  [config]
  (reset! server/system (ig/init (merge server/config config))))
