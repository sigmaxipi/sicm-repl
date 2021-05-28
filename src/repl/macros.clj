(ns repl.macros
  (:require [cljs.env :as env])
  (:require [sicmutils.env]))

(defmacro analyzer-state [[_ ns-sym]]
  `'~(get-in @env/*compiler* [:cljs.analyzer/namespaces ns-sym]))

(defmacro overrideCore []
  "(ns repl.macro)
    (defmacro overrideCore []
      '(do
        (ns-unmap 'cljs.core '+) (ns-unmap 'repl.core '+) (def + sicmutils.env/+)
                                 (ns-unmap 'repl.core '-) (def - sicmutils.env/-)
        (ns-unmap 'cljs.core '*) (ns-unmap 'repl.core '*) (def * sicmutils.env/*)
        (ns-unmap 'cljs.core '/) (ns-unmap 'repl.core '/) (def / sicmutils.env//)))")

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
  (repl.macros/importUnary [literal-function])
  (repl.macros/importVariadic [asin atan compose cos cube D F->C Gamma Lagrange-equations simplify sin square up velocity + - * /])))
