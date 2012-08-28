(defproject clj-exceptional "0.7.1"
  :description "Clojure client for Exceptional http://getexceptional.com"
  :url "https://github.com/zkim/clj-exceptional"
  :dependencies [[cheshire "4.0.1"]
                 [clj-http "0.4.0"]]
  :profiles {:dev {:dependencies [[swank-clojure "1.2.0"]
                                  [lein-clojars "0.6.0"]]}})
