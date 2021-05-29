(ns repl.macros
  (:require [cljs.env :as env])
  (:require [sicmutils.env]))

; Load analyzer cache to allow symbol lookups.
(defmacro analyzer-state [[_ ns-sym]]
  `'~(get-in @env/*compiler* [:cljs.analyzer/namespaces ns-sym]))

; Import sicmutils.env similar to sicmutils.env/bootstrap-repl!
(defmacro importUnary [vals]
  `(do 
    ~@(for [v vals]
      `(defn ~v [~'x] (~(symbol "sicmutils.env" (str v)) ~'x)))))

(defmacro importBinary [vals]
  `(do 
    ~@(for [v vals]
      `(defn ~v [~'x ~'y] (~(symbol "sicmutils.env" (str v)) ~'x ~'y)))))

(defmacro importVariadic [vals]
  `(do 
    ~@(for [v vals]
      `(defn ~v [& ~'xs] (apply ~(symbol "sicmutils.env" (str v)) ~'xs)))))

(defmacro bootstrap-env! [] '(do
  (repl.macros/importUnary [])
  (repl.macros/importVariadic [asin atan compose cos cube D F->C Gamma Lagrange-equations simplify sin square up velocity])))

; If (ns ...) (require ...) is called in the evaluation snippet, the math operators are overridden. This restores the bindings.
(defmacro loadReplMacro []
  "(ns repl.macroEval)
    (defmacro overrideCore []
      '(do
        (ns-unmap 'cljs.core '+) (ns-unmap 'repl.core '+) (def + sicmutils.env/+)
                                 (ns-unmap 'repl.core '-) (def - sicmutils.env/-)
        (ns-unmap 'cljs.core '*) (ns-unmap 'repl.core '*) (def * sicmutils.env/*)
        (ns-unmap 'cljs.core '/) (ns-unmap 'repl.core '/) (def / sicmutils.env//)))
        
        
  (defmacro literal-function
    ([f] `(sicmutils.abstract.function/literal-function ~f))
    ([f sicm-signature]
    (if (and (list? sicm-signature)
              (= '-> (first sicm-signature)))
      `(sicmutils.abstract.function/literal-function ~f '~sicm-signature)
      `(sicmutils.abstract.function/literal-function ~f ~sicm-signature)))
    ([f domain range] `(sicmutils.abstract.function/literal-function ~f ~domain ~range)))           
        ")