(ns tces.smr-test
  (:require [clojure.test :refer [deftest is testing]]
            [tces.smr :as smr]))

(deftest forty-mwe-thermal-power
  (is (= 40.0 (:electric-power-mwe smr/default-40mwe-pwr))
      "Scenario uses 40 MWe")
  (let [p-th (smr/thermal-power-mwth smr/default-40mwe-pwr)]
    (is (< 115 p-th 130)
        (str "P_th ≈ 121 MW, got " p-th))))

(deftest steam-hotter-than-salt-charge
  (testing "280–320 °C steam requires interface loop, not direct coupling"
    (let [f (smr/interface-feasibility smr/default-40mwe-pwr)]
      (is (false? (:direct-coupling-to-steam? f)))
      (is (:interface-feasible? f))
      (is (>= (:interface-loop-t-c smr/default-40mwe-pwr)
              (:t-charge-required-c f))))))

(deftest recommends-upgrade-for-150c-delivery
  (is (= :upgrade (smr/recommend-mode smr/default-40mwe-pwr))))

(deftest surplus-energy-scale
  (let [gj (smr/surplus-energy-gj smr/default-40mwe-pwr)]
    (is (< 350 gj 450) (str "surplus window ~392 GJ, got " gj))))

(deftest sizing-positive-mass
  (let [{:keys [m-prs-kg inventory-scale]} (smr/size-tces-inventory smr/default-40mwe-pwr)]
    (is (> m-prs-kg 1000.0))
    (is (> inventory-scale 100.0))))
