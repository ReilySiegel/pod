(ns com.reilysiegel.pod.server.parser
  (:require [com.reilysiegel.pod.score :as score]
            [com.reilysiegel.pod.server.database.session :as db.session]
            [com.reilysiegel.pod.task :as task]
            [com.wsscode.pathom3.connect.indexes :as pci]
            [com.wsscode.pathom3.connect.operation :as pco]
            [com.wsscode.pathom3.interface.eql :as p.eql]
            [com.wsscode.pathom3.plugin :as p.plugin]
            [integrant.core :as ig]
            [com.reilysiegel.pod.person :as person]
            [com.wsscode.pathom.viz.ws-connector.pathom3 :as p.connector]
            [com.wsscode.pathom.viz.ws-connector.core :as pvc]))

(p.plugin/defplugin throw-mutation-errors-plugin
  {:com.wsscode.pathom3.format.eql/wrap-map-select-entry
   (fn [original]
     (fn [env source {:keys [key] :as ast}]
       (if (and (identical? :com.wsscode.pathom3.connect.runner/mutation-error
                            key)
                (contains? source key))
         (throw (key source))
         (original env source ast))))})

(p.plugin/defplugin resolve-in-mutations
  {:com.wsscode.pathom3.connect.runner/wrap-mutate
   (fn [mutate]
     (fn [env
         {:keys [params key]
          :as   ast}]
       (let [{{::pco/keys [input]}
              :config}
             (deref (resolve key))]
         (if-not input
           (mutate env ast)
           (mutate env (update ast
                               :params
                               merge
                               (p.eql/process env params input)))))))})

(p.plugin/defplugin global-resolve-in-mutations
  {:com.wsscode.pathom3.connect.runner/wrap-mutate
   (fn [mutate]
     (fn [env
         {:keys [params key]
          :as   ast}]
       (let [{{::pco/keys [global-input]}
              :config}
             (deref (resolve key))]
         (if-not global-input
           (mutate env ast)
           (mutate env (update ast
                               :params
                               merge
                               (p.eql/process env global-input)))))))})

(defn indexes [env]
  (-> env
      (p.plugin/register throw-mutation-errors-plugin)
      (p.plugin/register resolve-in-mutations)
      (p.plugin/register global-resolve-in-mutations)
      (pci/register (flatten [(score/resolvers)
                              (db.session/resolvers)
                              (person/resolvers)
                              (task/resolvers)]))
      (p.connector/connect-env {::pvc/parser-id "pod"})))

(defmethod ig/init-key ::env [_ opts]
  (indexes opts))

