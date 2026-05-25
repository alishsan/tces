(ns tces.yan-2020-test
  (:require [clojure.test :refer [deftest is testing]]
            [tces.modes :as modes]
            [tces.properties :as props]
            [tces.reactor :as reactor]
            [tces.thermo :as th]))

(def base-cycle (merge props/default-cycle {:x 0.85 :mu 8}))

(deftest reaction-heat-scales-with-x
  (is (= 5100.0 (th/reaction-heat 0.85 100.0 60.0))))

(deftest direct-mode-near-paper-cop
  (testing "Direct thermal energy storage & release (Table 1, X=0.85, μ=8)"
    (let [{:keys [cop-h gamma-h-kj-kg]} (modes/mode-result :direct base-cycle)]
      (is (< (Math/abs (- cop-h 0.973)) 0.03)
          (str "COPh=" cop-h))
      (is (< (Math/abs (- gamma-h-kj-kg 1692.84)) 120.0)
          (str "γh=" gamma-h-kj-kg)))))

(deftest upgrade-mode-near-paper-cop
  (let [{:keys [cop-h gamma-h-kj-kg]} (modes/mode-result :upgrade base-cycle)]
    (is (< (Math/abs (- cop-h 0.550)) 0.03))
    (is (< (Math/abs (- gamma-h-kj-kg 1427.82)) 80.0))))

(deftest sweep-monotonic-cop-with-x
  (let [cops (map (fn [{:keys [modes]}] (:cop-h (:direct modes)))
                  (reactor/sweep-x 8 :from 0.1 :to 1.0 :step 0.1))]
    (is (= (sort cops) cops))))
