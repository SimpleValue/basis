(ns basis.util.marked-vars
  "Allows to find Clojure vars, which have a given marker in its
   metadata.")

(defonce ^:private cache
  (atom {}))

(defn find-marked
  "Returns a set with all loaded Clojure vars, which have the given
   `marker` in their metadata.

   Caches the results. Uses `clojure.lang.Var/rev` for cache
   invalidation."
  [marker]
  (or
   (get-in @cache
           [marker
            clojure.lang.Var/rev])
   (let [rev clojure.lang.Var/rev
         result (into #{}
                      (comp (mapcat (comp vals
                                          ns-map))
                            (filter (fn [var]
                                      (get (meta var)
                                           marker))))
                      (all-ns))]
     (get-in (swap! cache
                    assoc marker {rev result})
             [marker
              rev]))))

(comment
  ;; The second invocation will be much faster, since the `cache` is
  ;; used:
  (time (count (find-marked :doc)))
  ;; After you load a namespace or change a var in another way the
  ;; first call will be slow again.
  )
