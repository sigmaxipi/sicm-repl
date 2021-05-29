; Working namespace where user code is evaluated.
(ns repl.eval
  (:require
    [sicmutils.env]
    [sicmutils.expression.render :as render :refer [->TeX]])
    (:require-macros [repl.macros]))

; This overrides the `ns repl.eval` above and is used by the evaulation environment.
(def bootstrapString"
      (ns repl.eval
        (:require [sicmutils.abstract.function]
                  [sicmutils.env :as S] )
        (:require-macros [repl.eval-macros :refer [literal-function]]))
      (repl.eval-macros/overrideCore)")

; Pull sicmutils.env into this namespace. repl.core calls (cljs.js/load-analysis-cache! repl.eval)
; to make the items in this namespace available to the evaluation environment
(repl.macros/bootstrap-env!)   

; Utilities to interact with JS side.
(defn pTeX [ex]
  (let [s (->TeX (simplify ex))]
    (js/outputTex s)))

