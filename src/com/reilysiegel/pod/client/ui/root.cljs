(ns com.reilysiegel.pod.client.ui.root
  (:require [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.reilysiegel.pod.client.ui.app-bar :as ui.app-bar]
            [com.reilysiegel.pod.client.ui.material :as mui]
            [com.reilysiegel.pod.client.ui.notification :as notification]))

(defsc Root [_this {::keys [app-bar notifier]
                    :as    props}]
  {:query         [{::app-bar (comp/get-query ui.app-bar/AppBar)}
                   {::notifier (comp/get-query notification/Notifier)}]
   :initial-state {::app-bar  {}
                   ::notifier {}}
   :use-hooks?    true}
  (mui/styles-provider
   {:injectFirst true}
   (mui/theme-provider
    {:theme (mui/create-mui-theme
             #_{:palette {:mode
                          (if (mui/use-media-query "(prefers-color-scheme: dark)")
                            :dark
                            :light)
                          :primary   {:main "#673AB7"}
                          :secondary {:main "#FF5722"}}}
             {:palette {:mode       :dark
                        :secondary  {:main         "#81A1C1"
                                     :contrastText "#ECEFF4"}
                        :primary    {:light        "#8FBCBB"
                                     :main         "#88C0D0"
                                     :dark         "#5E81AC"
                                     :contrastText "#ECEFF4"}
                        :error      {:main         "#BF616A"
                                     :contrastText "#ECEFF4"}
                        :warning    {:main         "#EBCB8B"
                                     :contrastText "#ECEFF4"}
                        :info       {:main         "#B48EAD"
                                     :contrastText "#ECEFF4"}
                        :success    {:main         "#A3BE8C"
                                     :contrastText "#ECEFF4"}
                        :text       {:primary   "#ECEFF4"
                                     :secondary "#E5E9F0"}
                        :background {:paper   "#3B4252"
                                     :default "#2E3440"}}})}
    (comp/fragment
     (mui/css-baseline)
     ;; Includes router
     (ui.app-bar/ui-app-bar app-bar)
     (notification/ui-notifier notifier)))))
