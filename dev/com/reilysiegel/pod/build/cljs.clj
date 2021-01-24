(ns com.reilysiegel.pod.build.cljs
  (:require [shadow.cljs.devtools.api :as api]
            [shadow.cljs.devtools.server :as server]))

(defn watch
  {:shadow/requires-server true}
  [{:keys [build-id]
    :as   opts}]
  (server/start!)
  (api/watch build-id opts)
  (server/wait-for-stop!))
