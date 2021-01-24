(ns com.reilysiegel.pod.client.ui.app-bar
  (:require [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.mutations :as m]
            [com.fulcrologic.fulcro.routing.dynamic-routing
             :as
             dr
             :refer
             [defrouter]]
            [com.fulcrologic.fulcro.ui-state-machines :as uism]
            [com.reilysiegel.pod.client.routing :as routing]
            [com.reilysiegel.pod.client.session :as session]
            [com.reilysiegel.pod.client.ui.busy :as busy]
            [com.reilysiegel.pod.client.ui.dashboard :as dashboard]
            [com.reilysiegel.pod.client.ui.login :as login]
            [com.reilysiegel.pod.client.ui.material :as mui]
            [com.reilysiegel.pod.client.ui.person :as ui.person]
            [com.reilysiegel.pod.client.ui.task :as ui.task]
            [com.reilysiegel.pod.person :as person]))

(defrouter RootRouter [_this _props]
  {:router-targets [login/LoginForm login/SignupForm dashboard/Dashboard
                    ui.person/PersonList ui.person/Person ui.task/TaskList]})

(def ui-root-router (comp/factory RootRouter))

(def drawer-width 240)

(def use-styles
  (mui/make-styles
   (fn [{:keys [spacing zIndex mixins transitions] :as _styles}]
     (let [{:keys [create easing duration]} transitions]
       {:root         {:flexGrow 1
                       :position :fixed
                       :width    "100%"}
        :app-bar      {:zIndex (inc (:drawer zIndex))}
        :drawer-inner {:marginTop (get-in mixins [:toolbar :minHeight])}
        :title        {:flexGrow 1}
        :container
        {:marginTop  (get-in mixins [:toolbar :minHeight])
         :paddingTop (spacing 3)
         :transition (create "margin"
                             #js {:easing   (:easeOut easing)
                                  :duration (:enteringScreen duration)})} 
        :container-shift
        {:marginTop  (get-in mixins [:toolbar :minHeight])
         :paddingTop (spacing 3)
         :marginLeft drawer-width
         :transition (create "margin"
                             #js {:easing   (:easeOut easing)
                                  :duration (:enteringScreen duration)})}}))))

(defsc AppBar [this {:ui/keys [open?]
                     ::keys   [current-user router busy]}]
  {:ident         (fn [] [:component/id ::app-bar])
   :query         [:ui/open?
                   {[::session/session :current-user]
                    [::person/id]}
                   {::current-user (comp/get-query session/CurrentUser)}
                   {::router (comp/get-query RootRouter)}
                   {::busy (comp/get-query busy/Busy)}]
   :initial-state {::current-user {}
                   :ui/open?      false
                   ::router       {}
                   ::busy         {}}
   :pre-merge     (fn [{:keys [data-tree]}]
                    (merge (comp/get-initial-state AppBar)
                           data-tree))
   :use-hooks?    true}
  (let [{:keys [root title app-bar drawer-inner container
                container-shift]
         :as   _styles}
        (use-styles)
        ui-open?      (boolean (and open? (::person/id current-user)))
        session-state (uism/get-active-state this ::session/sessions)
        ready?        (and session-state
                           (not (#{:state/checking-existing-session :initial}
                                 session-state)))]
    (comp/fragment
     (dom/div
      {:className root}
      (mui/app-bar
       {:className app-bar
        :color     :inherit}
       (mui/toolbar
        {}
        (mui/icon-button
         {:sx      {:mr 1
                    :p  1}
          :edge    :start
          :color   :inherit
          :onClick (when (::person/id current-user)
                     #(m/toggle! this :ui/open?))}
         (mui/menu-icon {}))
        (mui/typography {:variant "h6" :className title}
                        "Pod")
        (session/ui-current-user current-user))
       (busy/ui-busy busy))
      (mui/drawer
       {:open    ui-open?
        :variant :persistent
        :anchor  :left}
       (mui/box
        {:sx        {:pt    1
                     :width drawer-width}
         :className drawer-inner}
        (mui/list
         {}
         (for [{:keys [:ui.menu/label :ui.menu/icon :route-segment]}
               (->> [::dr/id ::RootRouter]
                    (comp/ident->any this)
                    comp/component-options
                    :router-targets
                    (map comp/component-options)
                    (filter :ui.menu/label)
                    (sort-by :ui.menu/order))]
           (mui/box {:sx    {:p  1
                             :pr 2
                             :pl 2}
                     :clone true
                     :key   label}
                    (mui/list-item
                     {:button  true
                      :onClick #(comp/transact! this [(routing/route-to
                                                       {:route route-segment})])}
                     (when icon
                       (mui/list-item-icon {} (icon)))
                     (mui/list-item-text {:primary label}))))))))
     (when ready?
       (dom/div
        {:className (if ui-open?
                      container-shift
                      container)}
        (mui/container {}
                       (ui-root-router router)))))))

(def ui-app-bar (comp/factory AppBar))
