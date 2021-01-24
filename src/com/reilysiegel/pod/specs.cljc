(ns com.reilysiegel.pod.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::non-empty-string (s/and string?
                                 (complement str/blank?)))
