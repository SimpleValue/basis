(ns sv.basis.kubernetes.secret
  (:require [babashka.process :as process]
            [cheshire.core :as json]))

(defn get-secret
  [{:keys [secret-name]}]
  (-> (process/process
        ["kubectl" "get" "secret" secret-name "-o" "json"]
        {:out :string})
      (process/check)
      (:out)
      (json/parse-string true)))

(defn save!
  [secret]
  (process/check
    (process/process
      ["kubectl" "apply" "-f" "-"]
      {:in (json/generate-string secret)})))

(defn edit!
  [params f & args]
  (let [secret (get-secret params)
        new-secret (apply f secret args)]
    (save! new-secret)))

(defn base64-encode [to-encode]
  (.encodeToString (java.util.Base64/getUrlEncoder)
                   to-encode))

(defn- base64-decode [to-decode]
  (.decode (java.util.Base64/getDecoder)
           to-decode))

(defn base64-vals
  [a-map]
  (into {}
        (map (fn [[key value]]
               [key
                (base64-encode
                 (.getBytes value
                            "UTF-8"))])
             a-map)))

(defn string-vals
  [a-map]
  (into {}
        (map (fn [[key value]]
               [key
                (String. (base64-decode value)
                         "UTF-8")])
             a-map)))

(defn merge-key-values
  [secret key-values]
  (update-in secret
             [:data]
             merge
             (base64-vals key-values)))

(defn set-key-value
  [secret key value]
  (merge-key-values secret
                    {key value}))

(defn get-key-values
  [params]
  (-> params
      (get-secret)
      (:data)
      string-vals))

(comment
  (get-secret {:secret-name "app-env"})

  (get-key-values {:secret-name "app-env"})
  )
