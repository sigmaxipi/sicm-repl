Dev build:
 `clj -M -m cljs.main  --repl-opts "{:launch-browser false}" -c repl.core --repl`
Prod build:
 `rm -r out; time clj -M -m cljs.main -co build.edn -c repl.core && cat out/cljsjs/fraction/production/bigfraction.min.inc.js out/main.js > out/main.js2 && mv out/main.js2 out/main.js  &&  clj -M -m cljs.main  --serve`