(ns com.reilysiegel.pod.score
  (:require [kixi.stats.math :as ksm]
            [com.reilysiegel.pod.person :as person]
            [com.wsscode.pathom3.connect.operation :as pco]
            [com.reilysiegel.pod.task :as task]))

(def ^:dynamic *prior*
  "The default base beta distribution which is added to all score calculations.
  This  prior distribution  ensures  that a  small number  of  results does  not
  significantly impact the  results. The default values  represent a semi-strong
  assumption of ~66%."
  {::alpha 10
   ::beta  5})

(def ^:dynamic *effort-fn*
  "The default effort function."
  ksm/sqrt)

(defn effort-adjusted-params
  "Computes a beta distribution to represent a task with effort `effort` that is
  `complete?` and `late?`."
  [{::task/keys [effort complete? late?]}]
  (case [(boolean complete?) (boolean late?)]
    [true false] {::alpha (*effort-fn* effort)}
    [true true]  {::alpha (/ (*effort-fn* effort)
                             2)
                  ::beta  (/ (*effort-fn* effort)
                             2)}
    [false true] {::beta (*effort-fn* effort)}
    ;; Assume assigned tasks are completed.
    {::alpha (*effort-fn* effort)}))

(defn beta-between
  "Computes the probability that the true probability represented by a beta
  distribution is between `lower-bound` and `upper-bound`."
  [{::keys [alpha beta]} lower-bound upper-bound]
  (- (ksm/ibeta upper-bound alpha beta)
     (ksm/ibeta lower-bound alpha beta)))

(defn merge-beta-params
  "Merges a seq of `params`."
  [params]
  (apply merge-with + *prior* params))

(defn ** [x n]
  (reduce * (repeat n x)))

(defn B [{::keys [alpha beta]}]
  (/ (* (ksm/gamma alpha) (ksm/gamma beta))
     (ksm/gamma (+ alpha beta))))

(defn pdf [{::keys [alpha beta] :as dist}
           x]
  (/ (* (** x (dec alpha)) (** (- 1 x) (dec beta)))
     (B dist)))

(pco/defresolver score [{::person/keys [tasks]}]
  {::pco/priority 10000
   ::pco/input    [{::person/tasks
                    [(pco/? ::task/complete?)
                     (pco/? ::task/late?)
                     ::task/effort]}]
   ::pco/output   [::alpha ::beta]}
  (merge-beta-params (mapv effort-adjusted-params tasks)))

(pco/defresolver score-default []
  {::pco/input  [::person/id]
   ::pco/output [::alpha ::beta]}
  *prior*)


(pco/defresolver score-mean [{::keys [alpha beta]}]
  {::mean #?(:clj (float (/ alpha (+ alpha beta)))
             :default (/ alpha (+ alpha beta)))})

(defn resolvers
  "Return the list of Pathom resolvers related to scores."
  []
  [
   score
   score-default
   score-mean])


(comment
  (binding [*prior* {::alpha 10
                     ::beta  5}]
    (beta-between (merge-beta-params
                   (flatten
                    [(repeat 12 (effort-adjusted-params 15 true false))
                     (repeat 0 (effort-adjusted-params 15 true true))
                     (repeat 2 (effort-adjusted-params 15 false true))
                     (repeat 6 (effort-adjusted-params 90 true false))
                     (repeat 0 (effort-adjusted-params 90 true true))
                     (repeat 0 (effort-adjusted-params 90 false true))]))
                  0.9
                  1)))
