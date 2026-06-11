(ns tces.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [tces.core :as tces]))

(deftest simulate-all-modes
  (let [results (tces/simulate {:x 0.85 :mu 8})]
    (is (contains? results :direct))
    (is (contains? results :upgrade))
    (is (contains? results :combined))))

(deftest validate-passes
  (is (:pass? (tces/validate))))

(deftest smr-default-report
  (let [r (tces/smr-report tces/default-40mwe-pwr)]
    (is (= 40.0 (get-in r [:scenario :electric-power-mwe])))
    (is (= :upgrade (get-in r [:sizing :recommended-mode])))))

(deftest mode-result-matches-simulate
  (let [state {:x 0.85 :mu 8}
        direct (tces/mode-result :direct state)
        all (tces/simulate state)]
    (is (= (:cop-h direct) (:cop-h (:direct all))))))
