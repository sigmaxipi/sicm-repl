; Working namespace where user code is evaluated.
(ns repl.eval
  (:require [clojure.string] [sicmutils.env] [sicmutils.expression.render])
  (:require-macros [repl.macros]))

; This overrides the `ns repl.eval` above and is used by the evaulation environment.
; Note that macros aren't pulled in but are defined seperately in repl.eval-macros
(def sicm-symbols (clojure.string/join " " (map (fn [s] (first s)) (ns-publics 'sicmutils.env))))
(def bootstrap-string
  (str "
    (ns repl.eval
        (:require [sicmutils.abstract.function] [sicmutils.env :refer [" sicm-symbols "]])
        (:require-macros [repl.eval-macros :refer [literal-function]]))
    (println \"FIXME: THIS CODE ISNT RUNNING WHEN SICMUTILS HAS A :refer BLOCK SINCE THE SOURCE CODE IS FAKED BY *load-fn*.\")
    (repl.eval-macros/overrideCore)"))

; Utilities to interact with JS side.
(defn pTeX [ex]
  (let [s (sicmutils.expression.render/->TeX (sicmutils.env/simplify ex))]
    (js/outputTex s)))

