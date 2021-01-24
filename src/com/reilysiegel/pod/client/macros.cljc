(ns com.reilysiegel.pod.client.macros
  #?(:cljs (:require-macros [com.reilysiegel.pod.client.macros])))

(defmacro defmui [name component]
  (def ~name
    (com.fulcrologic.fulcro.algorithms.react-interop/react-factory ~component)))
