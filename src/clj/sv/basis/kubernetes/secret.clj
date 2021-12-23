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
  [params f]
  (-> (get-secret params)
      (f)
      (save!)))

(defn base64-encode [to-encode]
  (.encodeToString (java.util.Base64/getUrlEncoder)
                   to-encode))

(defn set-key-value!
  [{:keys [key value] :as params}]
  (edit! params
         (fn [secret]
           (assoc-in secret
                     [:data
                      key]
                     (base64-encode (.getBytes value
                                               "UTF-8"))))))

(comment
  (get-secret {:secret-name "app-env"})

  )
