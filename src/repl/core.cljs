(ns repl.core
  (:require
    [cljs.analyzer]
    [cljs.js :refer [compile-str empty-state eval-str js-eval]]
    [cljs.reader]
    [cljs.tools.reader.reader-types]
    [sicmutils.abstract.function]
    [sicmutils.env :as env :include-macros true]
    [sicmutils.expression.render :as render :refer [->TeX]])
  (:require-macros [repl.macros] [sicmutils.env])
)

(repl.macros/bootstrap-env!)

(defn pTeX [ex]
  (let [s (->TeX (simplify ex))]
    (js/outputTex s)))

; Allow (require sicmutils.env) to work.
(defn loader  [opts cb]
  (if (= (:name opts) 'repl.macroEval)
    (do
      (println "(repl.macroEval loader " opts ")" (:name opts))
      (cb {:lang :clj :source (repl.macros/loadReplMacro)}))
    (do
      (println "(dummy-loader " opts ")" (:name opts))
      (cb {:lang :clj :source ""}))
  ))

; Break up the input so that it can be evaluated one form at a time.
; This allows for better error handling and extraction of the final value.
(defn split-into-expressions [source]
  (loop [string source
         exprs []]
    (if-not string
      exprs
      (let [trimmed-string (clojure.string/replace string #"^[\s\n]*" "")
            sentinel (js-obj)
            [obj expr] (cljs.tools.reader/read+string {:eof sentinel} (cljs.tools.reader.reader-types/source-logging-push-back-reader trimmed-string))]
        (if (= sentinel obj) exprs (recur (subs trimmed-string (+ 1 (count expr))) (conj exprs expr)))))))

; Actual eval code.
(def state (cljs.js/empty-state))

(defn ^:export evalStr [source, compile-cb, eval-cb]
  ; Compile the entire source to check for syntax errors.
  (compile-str
    state
    source
    "compileStr"
    { :context    :statement
      :def-emits-var true
      :eval       js-eval
      :load       loader
      :ns 'repl.core
      :source-map true}
    ; Handle result
    (fn [compile-result]
      (compile-cb (clj->js compile-result))
      (if-not (:error compile-result)
        ; Evaluate the source one expression at a time.
        (let [*eval-result* (atom nil)]
          (loop [exprs (split-into-expressions source)]
            (if exprs
              (do
                (eval-str
                  state
                  (first exprs)
                  "evalStr"
                  { :context    :expr
                    :eval       js-eval
                    :load       loader
                    :ns 'repl.core
                    :source-map true}
                  (fn [eval-result] (reset! *eval-result* eval-result)))
                  (if (:error @*eval-result*)
                    (eval-cb (first exprs) (clj->js @*eval-result*))
                    (recur (next exprs))))))
            ; Send final value if we didn't error out.
            (if-not (:error @*eval-result*) (eval-cb "" (clj->js @*eval-result*))))))))

; Load 'repl.core symbols into the evalution state.
(cljs.js/load-analysis-cache! state 'repl.core (repl.macros/analyzer-state 'repl.core))

; Reinit 'repl.core as an evaulation namespace.
(defn init-state [state] 
  (eval-str
    state "
      (ns repl.core
        (:require [sicmutils.abstract.function]
                  [sicmutils.env :as S] )
        (:require-macros [repl.macroEval :refer [literal-function]]))
      (repl.macroEval/overrideCore)"
    "initialEvalStr"
    { :context    :statement
      :eval       js-eval
      :load       loader
      :ns 'repl.core
      :source-map true}
    js/console.log))
(init-state state)

; Output strings to Javascript side.
(defn warning-handler [warning-type env extra]
  (js/logW (str (cljs.analyzer/message env (str "WARNING: " (cljs.analyzer/error-message warning-type extra))))))
(set! cljs.analyzer/*cljs-warning-handlers* [warning-handler])

(js/log "...CLJS loading complete.")
