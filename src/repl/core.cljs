(ns repl.core
  (:require
    [cljs.env :as cljs-env]
    [cljs.js :refer [compile-str empty-state eval-str js-eval]]
    [sicmutils.env :as sicm-env :include-macros true]
    [sicmutils.expression.render :as render :refer [->TeX]]
    [shadow.cljs.bootstrap.browser :as boot])
  (:require-macros [repl.macros])
)

(repl.macros/bootstrap-env!)

(defn pTeX [ex]
  (let [s (->TeX (sicm-env/simplify ex))]
    (js/outputTex s)))


(defonce state (cljs-env/default-compiler-env))

(def opts {
  :context statement
  :eval js-eval
  :load (partial boot/load state)
  :ns 'repl.core
  :source-map true})
(defn ^:export evalStr [source, cb]
  (compile-str state source "evalStr" opts println)
  (eval-str state source "evalStr" opts (fn [result] (cb (clj->js result)))))

(boot/init state {:path "out/bootstrap"} (fn []))

(cljs.js/load-analysis-cache! state 'repl.core (repl.macros/analyzer-state 'repl.core))
(js/log "CLJS loading complete.")
