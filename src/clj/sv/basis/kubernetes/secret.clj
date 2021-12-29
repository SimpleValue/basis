(ns sv.basis.kubernetes.secret
  "A tool to inspect and edit Kubernetes secrets. These are especially
   useful to manage environment variables:

   https://kubernetes.io/docs/tasks/inject-data-application/distribute-credentials-secure/#configure-all-key-value-pairs-in-a-secret-as-container-environment-variables

   Everything works via Kubernetes `kubectl` command line tool. Please
   make sure it is connected to the correct Kubernetes cluster or
   provide the cluster name in the params."
  (:require [babashka.process :as process]
            [cheshire.core :as json]))

(defn get-secret
  "Gets a Kubernetes secret by name.

   Optionally define the Kubernetes `:cluster` for which `kubectl`
   should be invoked."
  [{:keys [secret-name cluster]}]
  (-> (process/process
       (concat
        ["kubectl" "get" "secret" secret-name "-o" "json"]
        (when cluster
          ["--cluster" cluster]))
       {:out :string})
      (process/check)
      (:out)
      (json/parse-string true)))

(defn save!
  "Saves the Kubernetes `secret`."
  [{:keys [cluster]} secret]
  (process/check
   (process/process
    (concat
     ["kubectl" "apply" "-f" "-"]
     (when cluster
       ["--cluster" cluster]))
    {:in (json/generate-string secret)})))

(defn edit!
  "Edits a Kubernetes secret by getting its current value, applying the
   function `f` to it and saving the result as the secret's new value.

   `f` receives the Kubernetes secret and the `args` as
   aruments. Provide the `:secret-name` in the `params`. "
  [params f & args]
  (let [secret (get-secret params)
        new-secret (apply f secret args)]
    (save! params
           new-secret)))

(defn base64-encode
  [to-encode]
  (.encodeToString (java.util.Base64/getUrlEncoder)
                   to-encode))

(defn base64-decode
  [to-decode]
  (.decode (java.util.Base64/getDecoder)
           to-decode))

(defn base64-vals
  "Encodes all map values as base64."
  [a-map]
  (into {}
        (map (fn [[key value]]
               [key
                (base64-encode
                 (.getBytes value
                            "UTF-8"))])
             a-map)))

(defn string-vals
  "Decodes all map values from base64 into Strings."
  [a-map]
  (into {}
        (map (fn [[key value]]
               [key
                (String. (base64-decode value)
                         "UTF-8")])
             a-map)))

(defn merge-key-values
  "Merges the `key-values` into the `secret`'s data."
  [secret key-values]
  (update-in secret
             [:data]
             merge
             (base64-vals key-values)))

(defn set-key-value
  "Sets the `key` `value` in the `secret`'s data."
  [secret key value]
  (merge-key-values secret
                    {(keyword key)
                     value}))

(defn get-key-values
  "Returns the data of a secret and decodes all map values from base64
   into Strings."
  [params]
  (-> params
      (get-secret)
      (:data)
      string-vals))

(comment
  (get-secret {:secret-name "app-env"})

  (get-key-values {:secret-name "app-env"})
  )
