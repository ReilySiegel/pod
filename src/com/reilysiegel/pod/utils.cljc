(ns com.reilysiegel.pod.utils
  (:require [com.wsscode.pathom3.connect.operation :as pco]
            #?(:clj [datahike.core :as d]))
  #?(:clj (:import [java.util Base64 UUID])))

#?(:clj
   (defn pull-resolver
     "Defines a resolver that pulls from a given db."
     [eid-key selector]
     (pco/resolver (symbol (str (namespace eid-key) \/ (name eid-key) "-pull"))
                   {::pco/input  [eid-key]
                    ::pco/output selector}
                   (fn [{:com.reilysiegel.pod.server.database/keys [conn]}
                       input]
                     (let [eid-val (get input eid-key)]
                       (d/pull @conn selector [eid-key eid-val]))))))

(defn base64-decode [s]
  #?(:clj
     
     (String. (.decode (Base64/getDecoder) s))))

(defn base64-encode [s]
  #?(:cljs (js/btoa s)))

#?(:cljs
   (defn- to-hex-string [n l]
     (let [s (.toString n 16)
           c (count s)]
       (cond
         (> c l) (subs s 0 l)
         (< c l) (str (apply str (repeat (- l c) "0")) s)
         :else   s))))

(defn- rand-bits [pow]
  (rand-int (bit-shift-left 1 pow)))


(defn squuid
  "Generates a UUID that grow with time. Such UUIDs will always go to the end  of the index and that will minimize insertions in the middle.
  
   Consist of 64 bits of current UNIX timestamp (in seconds) and 64 random bits (2^64 different unique values per second)."
  ([]
   (squuid #?(:clj  (System/currentTimeMillis)
              :cljs (.getTime (js/Date.)))))
  ([msec]
   #?(:clj
      (let [uuid     (UUID/randomUUID)
            time     (int (/ msec 1000))
            high     (.getMostSignificantBits uuid)
            low      (.getLeastSignificantBits uuid)
            new-high (bit-or (bit-and high 0x00000000FFFFFFFF)
                             (bit-shift-left time 32))]
        (UUID. new-high low))
      :cljs
      (uuid
       (str
        (-> (int (/ msec 1000))
            (to-hex-string 8))
        "-" (-> (rand-bits 16) (to-hex-string 4))
        "-" (-> (rand-bits 16) (bit-and 0x0FFF) (bit-or 0x4000) (to-hex-string 4))
        "-" (-> (rand-bits 16) (bit-and 0x3FFF) (bit-or 0x8000) (to-hex-string 4))
        "-" (-> (rand-bits 16) (to-hex-string 4))
        (-> (rand-bits 16) (to-hex-string 4))
        (-> (rand-bits 16) (to-hex-string 4)))))))

(defn authed? [props]
  (-> props
      :com.reilysiegel.pod.client.session/session
      :current-user
      :com.reilysiegel.pod.person/id))
