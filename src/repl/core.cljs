(ns repl.core
  (:require
    [sicmutils.env :as env :include-macros true]
    [sicmutils.expression.render :as render :refer [->TeX]]
    [cljs.js :refer [compile-str empty-state eval-str js-eval]])
  (:require-macros [repl.macros] [sicmutils.env])
)

(repl.macros/bootstrap-env!)

(defn pTeX [ex]
  (let [s (->TeX (simplify ex))]
    (js/outputTex s)))

(defn loader  [opts cb]
  (println "(dummy-loader " opts ")")
    (cb {:lang :clj :source (repl.macros/overrideCore)}))

(def state (cljs.js/empty-state))
(def opts { :context    :expr
          :def-emits-var true
          :eval       js-eval
          :load       loader
          :ns 'repl.core
          :source-map true})
(defn ^:export evalStr [source, cb]
  (compile-str state source "evalStr" opts println)
  (eval-str state source "evalStr" opts (fn [result] (cb (clj->js result)))))

(cljs.js/load-analysis-cache! state 'repl.core (repl.macros/analyzer-state 'repl.core))
(js/log "CLJS loading complete.")
