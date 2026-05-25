(defproject tces "0.1.0-SNAPSHOT"
  :description "NiCl2-SrCl2/NH3 multi-mode thermochemical resorption heat transformer simulator"
  :url "https://github.com/example/tces"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.12.0"]]
  :main tces.main
  :aot [tces.main]
  :target-path "target"
  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.1"]]}}
  :test-paths ["test"])
