(ns com.reilysiegel.pod.person
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.test.check.generators :as tgen]
            [com.reilysiegel.pod.specs :as specs]
            [com.wsscode.pathom3.connect.operation :as pco]))

(s/def ::id uuid?)

(s/def ::email
  (s/with-gen
    (s/and string?
           (partial re-matches #".+@.+"))
    #(tgen/let [local (tgen/fmap str/join (tgen/vector tgen/char-ascii 1 64))
                domain (tgen/elements ["wpi.edu"
                                       "google.com"
                                       "crows.house"])]
       (tgen/return (str local \@ domain)))))

(s/def ::name ::specs/non-empty-string)

(s/def ::password ::specs/non-empty-string)

(s/def ::op? boolean?)

(s/def ::password2 ::password)

(s/def ::password-check (fn [{::keys [password password2]}]
                          (= password password2)))

(s/def ::password-hash ::specs/non-empty-string)

(s/def ::person (s/and (s/keys :req [::email ::password]
                               :opt [::password2])
                       ::password-check))

(pco/defresolver authed? [{{::keys [email]} ::authed}]
  {::pco/input [(pco/? ::authed)]}
  {::authed? (boolean email)})

(defn resolvers []
  [authed?])

(boolean nil)
