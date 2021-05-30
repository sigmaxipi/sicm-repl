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
                [sicmutils.env :as S :refer [asin acos atan compose cos cube D F->C Gamma Lagrange-equations simplify sin square up velocity]] )
    (:require-macros [repl.eval-macros :refer [literal-function]]))")

; Utilities to interact with JS side.
(defn pTeX [ex]
  (let [s (->TeX (sicmutils.env/simplify ex))]
    (js/outputTex s)))

