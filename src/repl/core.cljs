; Module that contains the primary evaluation framework.
(ns repl.core
  (:require
    ["fraction.js/bigfraction.js" :as Fraction]
    [sci.core :as sci]
    [sicmutils.env]
    [sicmutils.env.sci]
    [sicmutils.expression.render]))

;(def dummy (.div (.mul (Fraction 1 2) 3) 4)) ; Include to avoid build problems.

(defn pTeX [ex]
    (-> ex sicmutils.env/simplify sicmutils.expression.render/->TeX js/outputTex))

(def context-options {
  :namespaces sicmutils.env.sci/namespaces
  :bindings (
    merge {
      'pTeX pTeX
    }
    (sicmutils.env.sci/namespaces 'sicmutils.env))})
(def sicm-context  (sci/init context-options))

(defn ^:export evalStr [source eval-cb]
  (try 
    (eval-cb (clj->js {:value
      (sci/eval-string*  sicm-context source)}))
    (catch js/Error e 
      (eval-cb (clj->js {:error e})))))

(js/log "...CLJS loading complete.")
