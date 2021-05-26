(ns repl.core
  (:require
    [sicmutils.env :as env :include-macros true]
    [sicmutils.expression.render :as render :refer [->TeX]]
    [cljs.js :refer [compile-str empty-state eval-str js-eval]]
    [cljs.repl :as repl]
    [repl.lib])
  (:require-macros [repl.analysis :refer [analyzer-state]])
)
;(env/bootstrap-repl!)

(defn asin [x] (sicmutils.env/asin x))
(defn simplify [x] (sicmutils.env/simplify x))
(defn square [x] (sicmutils.env/square x))
(defn sin [x] (sicmutils.env/sin x))
(defn cube [x] (sicmutils.env/cube x))
(defn D [x] (sicmutils.env/D x))
(defn Lagrange-equations [x] (sicmutils.env/Lagrange-equations x))
(defn literal-function [x] (sicmutils.env/literal-function x))
(defn up [x y] (sicmutils.env/up x y))
(defn + [x y] (sicmutils.env/+ x y))

(def S sicmutils.env)

(defn pTeX [ex]
  (let [s (->TeX (env/simplify ex))]
    (js/outputTex s)))

    

(defn dummy-loader  [opts cb]
  (println "(dummy-loader " opts ")")
  (cb {:lang :clj :source "(println \"dummy-loader cb\")"}))

(def state (cljs.js/empty-state))
(def opts { :context    :expr
          :def-emits-var true
          :eval       js-eval
          :load       dummy-loader
          :ns 'repl.core
          :source-map true})
(defn ^:export evalStr [source, cb]
  (compile-str state source "evalStr" opts println)
  (eval-str state source "evalStr" opts (fn [result] (cb (clj->js result)))))

;(cljs.js/load-analysis-cache! state 'repl.lib (analyzer-state 'repl.lib))
(cljs.js/load-analysis-cache! state 'repl.core (analyzer-state 'repl.core))
;(cljs.js/load-analysis-cache! state 'sicmutils.abstract.function (analyzer-state 'sicmutils.abstract.function))
;(cljs.js/load-analysis-cache! state 'sicmutils.env (analyzer-state 'sicmutils.env))
(js/log "CLJS loading complete.");
;(js/console.log (type sicmutils.env/sin))
;(js/console.log (type sicmutils.generic/sin))
;(js/console.log (type sin))


;(js/console.log  sicmutils.env/sin)
;(js/console.log  sicmutils.generic/sin)
;(js/console.log  sin)

;(js/console.log (type repl.lib/libF))
;(js/console.log  repl.lib/libF)
;(js/console.log  (repl.lib/libF 1));

;(js/console.log (type repl.lib/libM))
;(js/console.log  repl.lib/libM)
;(js/console.log  (repl.lib/libM 1))
;(js/console.log  (repl.lib/libM 1 2))


(comment
  (pTeX (- (* 7 (/ 1 2)) 2))
  (pTeX (asin -10))

  (pTeX (square (sin (sicmutils.env/+ 'a 3))))

  (pTeX ((D cube) 'x))

  (defn L-central-polar [m U]
    (fn [[_ [r] [rdot thetadot]]]
      (sicmutils.env/- (sicmutils.env/* .5 m
            (sicmutils.env/+ (square rdot)
              (square (sicmutils.env/* r thetadot))))
        (U r))))
  (let [potential-fn (literal-function 'U)
        L     (L-central-polar 'm potential-fn)
        state (up (literal-function 'r)
                  (literal-function 'theta))]
    (pTeX
    (((Lagrange-equations L) state) 't)))

    

  (defn load-library-analysis-cache! []
    (cljs.js/load-analysis-cache! state 'library.core (analyzer-state 'library.core))
    nil)


  (pTeX "Hello world! 3")

  (defn quad [a b c]
    (let [d (Math.sqrt (- (* b b) (* 4 a c)))]
      [ (/ (+ (- 0 b) d) (* 2 a))
        (/ (- (- 0 b) d) (* 2 a))]))
  (quad 2 5 -3)
)