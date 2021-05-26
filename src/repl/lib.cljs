(ns repl.lib)

(defn libF [x] "F1")

(defmulti libM 
  (fn ([x & xs]  (+ 1 (count xs)))))
(defmethod libM 1 [x] "M1")
(defmethod libM 2 [x] "M2")

(defmacro libMac
  ([x] (println 1))
  ([x y] (println 2))
)