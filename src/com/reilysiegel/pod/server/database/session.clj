(ns com.reilysiegel.pod.server.database.session
  (:require [buddy.hashers :as ph]
            [com.fulcrologic.fulcro.server.api-middleware :as fmw]
            [com.reilysiegel.pod.person :as person]
            [com.reilysiegel.pod.utils :as util]
            [com.wsscode.pathom3.connect.operation :as pco]
            [datahike.api :as d]
            [clojure.string :as str]))

(pco/defresolver authed-user-basic [{:ring/keys [request]
                                     :com.reilysiegel.pod.server.database/keys
                                     [conn]} _]
  {::pco/output [{::person/authed [::person/email]}]}
  (let [[email pass] (-> request
                         :headers
                         (get "authorization")
                         (str/split #" ")
                         last
                         util/base64-decode
                         (str/split #":"))]
    (when (ph/check pass
                    (::person/password-hash (d/entity @conn
                                                      [::person/email
                                                       (str/lower-case email)])))
      {::person/authed {::person/email email}})))

(pco/defresolver authed-user-session [{:ring/keys [request]
                                       :com.reilysiegel.pod.server.database/keys
                                       [conn]} _]
  {::pco/output [{::person/authed [::person/email]}]}
  {::person/authed (when-let [email (get-in request [:session ::person/email])]
                     {::person/email email})})

(defn response-updating-session
  "Uses `mutation-response` as the actual return value for a mutation, but also stores the data into the (cookie-based) session."
  [mutation-env mutation-response]
  (let [existing-session (some-> mutation-env :ring/request :session)]
    (fmw/augment-response
     mutation-response
     (fn [resp]
       (let [new-session (merge existing-session mutation-response)]
         (assoc resp :session new-session))))))

(pco/defmutation login [{:com.reilysiegel.pod.server.database/keys [conn]
                         :as                                       env}
                        {::person/keys [email password]}]
  {::pco/output [::person/email]}
  (if (ph/check password
                (::person/password-hash (d/entity @conn
                                                  [::person/email
                                                   (str/lower-case email)])))
    (response-updating-session env {::person/email email})
    (throw (ex-info "Invalid credentials" {::person/email email}))))

(pco/defmutation logout [env _params]
  {::pco/output [::person/email]}
  (response-updating-session env {::person/email nil}))


(pco/defmutation signup [{:com.reilysiegel.pod.server.database/keys [conn]}
                         {::person/keys [password id name email] :as person}]
  {::pco/output [::person/id]}
  (d/transact conn [#::person{:id            id
                              :name          name
                              :email         (str/lower-case email)
                              :password-hash (ph/derive password)}])
  {::person/id (::person/id person)})

(defn resolvers []
  [authed-user-session
   authed-user-basic
   login
   logout
   signup])
