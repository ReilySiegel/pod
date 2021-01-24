(ns com.reilysiegel.pod.client.ui.score
  (:require ["victory" :as v]
            [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
            [com.fulcrologic.fulcro.components :as comp]
            [com.reilysiegel.pod.client.ui.material :as mui]
            [com.reilysiegel.pod.person :as person]
            [com.reilysiegel.pod.score :as score]
            [com.fulcrologic.fulcro.dom :as dom]))

(def victory-chart (interop/react-factory v/VictoryChart))
(def victory-area (interop/react-factory v/VictoryArea))
(def victory-axis (interop/react-factory v/VictoryAxis))

(def xs (range 0 1.01 0.01))

(defn data [dist x]
  {:x x
   :y (score/pdf dist x)})

(comp/defsc ScoreGraph [this {::score/keys  [alpha beta]
                              ::person/keys [id]
                              :as           props}]
  {:ident         ::person/id
   :query         [::score/alpha ::score/beta ::person/id]
   :initial-state {}
   :use-hooks?    true}  
  (let [data                        (map (partial data props) xs)
        {:keys [palette]}           (mui/use-theme)
        {:keys [primary secondary]} palette]
    ;; Issue with axis overlapping app bar
    #_
    (victory-chart
     {}
     (victory-area
      {:data  data
       :style {:data   {:fill        (:main primary)
                        :fillOpacity 0.5
                        :stroke      (:main primary)
                        :strokeWidth 3}
               :labels {}}})
     (victory-axis
      {:style     {:tickLabels {:fontSize      20
                                :stroke        "#fff"
                                :fill          "#fff"
                                :strokeOpacity 0.5
                                :fillOpacity   0.5}
                   :parent     {:z-index 1}}
       :crossAxis false}))
    (victory-area
     {:data  data
      :style {:data   {:fill        (:light secondary)
                       :stroke      (:main secondary)
                       :strokeWidth 3}
              :labels {}}})))

(def ui-score-graph (comp/factory ScoreGraph))
