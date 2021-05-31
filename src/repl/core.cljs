; Module that contains the primary evaluation framework.
(ns repl.core
  (:require
    [sci.core :as sci]
;    [sicmutils.env]
) )

(defn pTeX [ex]
;  (let [s (sicmutils.expression.render/->TeX (sicmutils.env/simplify ex))]
    (js/outputTex ex))

(defn ^:export evalStr [source eval-cb]
  (try 
    (eval-cb (clj->js {:value
      (sci/eval-string source  {:bindings {'pTeX pTeX}})}))
    (catch js/Error e (println "EXCEPTION " e))))
  



(js/log "...CLJS loading complete.")
