(ns repl.macros
  (:require [cljs.env])
  (:require [sicmutils.env]))

; Load analyzer cache from the app environment to the evaluation to allow symbol lookups.
(defmacro analyzer-state [[_ ns-sym]]
  `'~(get-in @cljs.env/*compiler* [:cljs.analyzer/namespaces ns-sym]))

; Initialize macros used by the evaluation environment. It can't directly use host macros.
(defmacro loadReplMacro []
  "(ns repl.eval-macros)
; If (ns ...) (require ...) is called in the evaluation snippet, the math operators are overridden. This restores the bindings.
    (defmacro overrideCore []
      '(do
        (ns-unmap 'cljs.core '+) (ns-unmap 'repl.core '+) (def + sicmutils.env/+)
                                 (ns-unmap 'repl.core '-) (def - sicmutils.env/-)
        (ns-unmap 'cljs.core '*) (ns-unmap 'repl.core '*) (def * sicmutils.env/*)
        (ns-unmap 'cljs.core '/) (ns-unmap 'repl.core '/) (def / sicmutils.env//)))
        
; Declare sicmutils.env macros.
  (defmacro literal-function
    ([f] `(sicmutils.abstract.function/literal-function ~f))
    ([f sicm-signature]
    (if (and (list? sicm-signature)
              (= '-> (first sicm-signature)))
      `(sicmutils.abstract.function/literal-function ~f '~sicm-signature)
      `(sicmutils.abstract.function/literal-function ~f ~sicm-signature)))
    ([f domain range] `(sicmutils.abstract.function/literal-function ~f ~domain ~range)))           
        ")