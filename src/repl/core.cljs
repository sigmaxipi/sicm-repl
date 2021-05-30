; Module that contains the primary evaluation framework.
(ns repl.core
  (:require
    [cljs.analyzer]
    [cljs.js :refer [compile-str empty-state eval-str js-eval]]
    [cljs.reader]
    [cljs.tools.reader.reader-types]
    [repl.eval])
)


; Allow (require sicmutils.env) to work.
(defn loader  [opts cb]
  (if (= (:name opts) 'repl.eval-macros)
    ; Inject the repl.eval-macros string into the evaluation environment.
    (do
      (println "(repl.eval-macros loader " opts ")" (:name opts))
      (cb {:lang :clj :source (repl.macros/loadReplMacro)}))
    ; Loading other libraries isn't supported. Instead they come in via load-analysis-cache.
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
      :ns 'repl.eval
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
                    :ns 'repl.eval
                    :source-map true}
                  (fn [eval-result] (reset! *eval-result* eval-result)))
                  (if (:error @*eval-result*)
                    (eval-cb (first exprs) (clj->js @*eval-result*))
                    (recur (next exprs))))))
            ; Send final value if we didn't error out.
            (if-not (:error @*eval-result*) (eval-cb "" (clj->js @*eval-result*))))))))

(defn init-state [state] 
  ; Load 'repl.core symbols into the evalution state.
  (cljs.js/load-analysis-cache! state 'repl.eval (repl.macros/analyzer-state 'repl.eval))
  ; Reinit 'repl.core as an evaulation namespace.
  (eval-str
    state
    repl.eval/bootstrap-string
    "initialEvalStr"
    { :context    :statement
      :eval       js-eval
      :load       loader
      :ns 'repl.eval
      :source-map true}
    (fn [result] (println "Evaluation namespace initialized.")))
  ; FIXME: This doesn't seem to run if it's part of the bootstramString above.
  (eval-str
    state
    "(repl.eval-macros/overrideCore)"
    "initialEvalStr"
    { :context    :statement
      :eval       js-eval
      :load       loader
      :ns 'repl.eval
      :source-map true}
    (fn [result] (println "Evaluation environment initialized."))))
(init-state state)

; Output strings to Javascript side.
(defn warning-handler [warning-type env extra]
  (js/logW (str (cljs.analyzer/message env (str "WARNING: " (cljs.analyzer/error-message warning-type extra))))))
(set! cljs.analyzer/*cljs-warning-handlers* [warning-handler])

(js/log "...CLJS loading complete.")
