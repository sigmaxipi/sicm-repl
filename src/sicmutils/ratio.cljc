;;
;; Copyright © 2017 Colin Smith.
;; This work is based on the Scmutils system of MIT/GNU Scheme:
;; Copyright © 2002 Massachusetts Institute of Technology
;;
;; This is free software;  you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation; either version 3 of the License, or (at
;; your option) any later version.
;;
;; This software is distributed in the hope that it will be useful, but
;; WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; General Public License for more details.
;;
;; You should have received a copy of the GNU General Public License
;; along with this code; if not, see <http://www.gnu.org/licenses/>.
;;

(ns sicmutils.ratio
  "This namespace provides a number of functions and constructors for working
  with ratios in Clojure and Clojurescript.

  [[clojure.lang.Ratio]] is native in Clojure. The Clojurescript implementation
  uses [Fraction.js](https://github.com/infusion/Fraction.js/).

  For other numeric extensions, see [[sicmutils.numbers]]
  and [[sicmutils.complex]]."
  #?(:clj
     (:refer-clojure :rename {rationalize core-rationalize
                              ratio? core-ratio?
                              denominator core-denominator
                              numerator core-numerator}))
  (:require #?(:clj [clojure.edn] :cljs [cljs.reader])
            #?(:cljs [goog.array :as garray])
            #?(:cljs [goog.object :as obj])
            [sicmutils.complex :as c]
            [sicmutils.expression :as x]
            [sicmutils.generic :as g]
            [sicmutils.util :as u]
            [sicmutils.value :as v]
            #?(:cljs ["fraction.js/bigfraction.js" :as Fraction]))
  #?(:clj (:import [clojure.lang BigInt Ratio])))

(def ^:no-doc ratiotype #?(:clj Ratio :cljs Fraction))
(derive ratiotype ::v/real)

(def ratio?
  #?(:clj core-ratio?
     :cljs (fn [r] (instance? Fraction r))))

(def numerator
  #?(:clj core-numerator
     :cljs (fn [^Fraction x]
             (if (pos? (obj/get x "s"))
               (obj/get x "n")
               (- (obj/get x "n"))))))

(def denominator
  #?(:clj core-denominator
     :cljs #(obj/get % "d")))

(defn- promote [x]
  (if (v/one? (denominator x))
    (numerator x)
    x))

(defn rationalize
  "Construct a ratio."
  ([x]
   #?(:cljs (if (v/integral? x)
              x
              (Fraction. x))
      :clj (core-rationalize x)))
  ([n d]
   #?(:cljs (if (v/one? d)
              n
              (promote (Fraction. n d)))
      :clj (core-rationalize (/ n d)))))

(def ^:private ratio-pattern #"([-+]?[0-9]+)/([0-9]+)")

(defn ^boolean matches? [pattern s]
  (let [[match] (re-find pattern s)]
    (identical? match s)))

(defn ^:private match-ratio
  [s]
  (let [m (vec (re-find ratio-pattern s))
        numerator   (m 1)
        denominator (m 2)
        numerator (if (re-find #"^\+" numerator)
                    (subs numerator 1)
                    numerator)]
    `(rationalize
      (u/bigint ~numerator)
      (u/bigint ~denominator))))

(defn parse-ratio
  "Parser for the `#sicm/ratio` literal."
  [x]
  (cond #?@(:clj
            [(ratio? x)
             `(rationalize
               (u/bigint ~(str (numerator x)))
               (u/bigint ~(str (denominator x))))])

        (v/number? x) `(sicmutils.ratio/rationalize ~x)
        (string? x)    (if (matches? ratio-pattern x)
                         (match-ratio x)
                         (recur
                          #?(:clj  (clojure.edn/read-string x)
                             :cljs (cljs.reader/read-string x))))
        :else (u/illegal (str "Invalid ratio: " x))))

#?(:clj
   (extend-type Ratio
     v/Numerical
     (numerical? [_] true)

     v/Value
     (zero? [c] (zero? c))
     (one? [c] (= 1 c))
     (identity? [c] (= 1 c))
     (zero-like [_] 0)
     (one-like [_] 1)
     (identity-like [_] 1)
     (freeze [x] (let [n (numerator x)
                       d (denominator x)]
                   (if (v/one? d)
                     n
                     `(~'/ ~n ~d))))
     (exact? [c] true)
     (kind [_] Ratio))

   :cljs
   (let [ZERO (Fraction. 0)
         ONE  (Fraction. 1)]
     (extend-type Fraction
       v/Numerical
       (numerical? [_] true)

       v/Value
       (zero? [c] (.equals c ZERO))
       (one? [c] (.equals c ONE))
       (identity? [c] (.equals c ONE))
       (zero-like [_] 0)
       (one-like [_] 1)
       (identity-like [_] 1)
       (freeze [x] (let [n (numerator x)
                         d (denominator x)]
                     (if (v/one? d)
                       (v/freeze n)
                       `(~'/
                         ~(v/freeze n)
                         ~(v/freeze d)))))
       (exact? [c] true)
       (kind [x] Fraction)

       IEquiv
       (-equiv [this other]
         (cond (ratio? other) (.equals this other)
               (v/integral? other)
               (and (v/one? (denominator this))
                    (v/= (numerator this) other))

               ;; Enabling this would work, but would take us away from
               ;; Clojure's behavior.
               #_(v/number? other)
               #_(.equals this (rationalize other))

               :else false))

       IComparable
       (-compare [this other]
         (if (ratio? other)
           (.compare this other)
           (let [o-value (.valueOf other)]
             (if (v/real? o-value)
               (garray/defaultCompare this o-value)
               (throw (js/Error. (str "Cannot compare " this " to " other)))))))

       Object
       (toString [r]
         (let [x (v/freeze r)]
           (if number? x)
           x
           (let [[_ n d] x]
             (str n "/" d))))

       IPrintWithWriter
       (-pr-writer [x writer opts]
         (let [n (numerator x)
               d (denominator x)]
           (if (v/one? d)
             (-pr-writer n writer opts)
             (write-all writer "#sicm/ratio \""
                        (str n) "/" (str d)
                        "\"")))))))

#?(:clj
   (doseq [[op f] [[g/exact-divide /]
                   [g/quotient quot]
                   [g/remainder rem]
                   [g/modulo mod]]]
     (defmethod op [Ratio Ratio] [a b] (f a b))
     (defmethod op [Ratio ::v/integral] [a b] (f a b))
     (defmethod op [::v/integral Ratio] [a b] (f a b)))

   :cljs
   (do
     (defn- pow [r m]
       (let [n (numerator r)
             d (denominator r)]
         (if (neg? m)
           (rationalize (g/expt d (g/negate m))
                        (g/expt n (g/negate m)))
           (rationalize (g/expt n m)
                        (g/expt d m)))))

     ;; The -equiv implementation handles equality with any number, so flip the
     ;; arguments around and invoke equiv.
     (defmethod v/= [::v/real Fraction] [l r] (= r l))

     (defmethod g/add [Fraction Fraction] [a b] (promote (.add a b)))
     (defmethod g/sub [Fraction Fraction] [a b] (promote (.sub a b)))
     (defmethod g/mul [Fraction Fraction] [^Fraction a ^Fraction b] (promote (.mul a b)))
     (defmethod g/div [Fraction Fraction] [^Fraction a ^Fraction b] (promote (.div a b)))
     (defmethod g/exact-divide [Fraction Fraction] [^Fraction a ^Fraction b] (promote (.div a b)))
     (defmethod g/negate [Fraction] [^Fraction a] (promote (.neg a)))
     (defmethod g/negative? [Fraction] [^Fraction a] (neg? (obj/get a "s")))
     (defmethod g/invert [Fraction] [^Fraction a] (promote (.inverse a)))
     (defmethod g/square [Fraction] [^Fraction a] (promote (.mul a a)))
     (defmethod g/cube [Fraction] [^Fraction a] (promote (.pow a 3)))
     (defmethod g/abs [Fraction] [^Fraction a] (promote (.abs a)))
     (defmethod g/magnitude [Fraction] [^Fraction a] (promote (.abs a)))
     (defmethod g/gcd [Fraction Fraction] [^Fraction a ^Fraction b] (promote (.gcd a b)))
     (defmethod g/lcm [Fraction Fraction] [^Fraction a ^Fraction b] (promote (.lcm a b)))
     (defmethod g/expt [Fraction ::v/integral] [a b] (pow a b))
     (defmethod g/sqrt [Fraction] [a]
       (if (neg? a)
         (g/sqrt (c/complex (.valueOf a)))
         (g/div (g/sqrt (u/double (numerator a)))
                (g/sqrt (u/double (denominator a))))))

     (defmethod g/modulo [Fraction Fraction] [^Fraction a ^Fraction b]
       (promote
        (.mod ^Fraction (.add ^Fraction (.mod a b) b) b)))

     ;; Only integral ratios let us stay exact. If a ratio appears in the
     ;; exponent, convert the base to a number and call g/expt again.
     (defmethod g/expt [Fraction Fraction] [a b]
       (if (v/one? (denominator b))
         (promote (.pow a (numerator b)))
         (g/expt (.valueOf a)
                 (.valueOf b))))

     (defmethod g/quotient [Fraction Fraction] [^Fraction a ^Fraction b]
       (promote
        (let [^Fraction x (.div a b)]
          (if (pos? (obj/get x "s"))
            (.floor x)
            (.ceil x)))))

     (defmethod g/remainder [Fraction Fraction] [^Fraction a ^Fraction b]
       (promote (.mod a b)))

     ;; Cross-compatibility with numbers in CLJS.
     (defn- downcast-fraction
       "Anything that `upcast-number` doesn't catch will hit this and pull a floating
  point value out of the ratio."
       [op]
       (defmethod op [Fraction ::v/real] [^Fraction a b]
         (op (.valueOf a) b))

       (defmethod op [::v/real Fraction] [a ^Fraction b]
         (op a (.valueOf b))))

     (defn- upcast-number
       "Integrals can stay exact, so they become ratios before op."
       [op]
       (defmethod op [Fraction ::v/integral] [a b]
         (op a (Fraction. b 1)))

       (defmethod op [::v/integral Fraction] [a b]
         (op (Fraction. a 1) b)))

     ;; An exact number should become a ratio rather than erroring out, if one
     ;; side of the calculation is already rational (but not if neither side
     ;; is).
     (upcast-number g/exact-divide)

     ;; We handle the cases above where the exponent connects with integrals and
     ;; stays exact.
     (downcast-fraction g/expt)

     (doseq [op [g/add g/mul g/sub g/gcd g/lcm
                 g/modulo g/remainder
                 g/quotient g/div]]
       (upcast-number op)
       (downcast-fraction op))))
