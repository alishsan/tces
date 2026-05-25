(ns tces.simulate
  (:require [tces.properties :as props]
            [tces.reactor :as reactor]
            [tces.thermo :as th]))

(defn compare-to-paper [results]
  (let [{:keys [x mu]} props/paper-benchmarks]
    (into {}
          (for [mode [:direct :upgrade :combined]
                :let [sim (get results mode)
                      ref (get props/paper-benchmarks mode)
                      cop-err (- (:cop-h sim) (:cop-h ref))
                      gam-err (- (:gamma-h-kj-kg sim) (:gamma-h-kj-kg ref))]]
            [mode {:simulated {:cop-h (float (:cop-h sim))
                               :gamma-h-kj-kg (float (:gamma-h-kj-kg sim))
                               :cop-c (when-let [c (:cop-c sim)] (float c))}
                   :paper ref
                   :delta {:cop-h (float cop-err)
                           :gamma-h-kj-kg (float gam-err)}}]))))

(defn within? [value target tolerance]
  (<= (Math/abs (- value target)) tolerance))

(defn validation-report []
  (let [results (reactor/make-system {:x 0.85 :mu 8})
        cmp (compare-to-paper results)]
    {:parameters {:x 0.85 :mu 8}
     :comparison cmp
     :pass?
     (and (within? (get-in cmp [:direct :simulated :cop-h]) 0.973 0.03)
          (within? (get-in cmp [:upgrade :simulated :cop-h]) 0.550 0.03)
          (within? (get-in cmp [:direct :simulated :gamma-h-kj-kg]) 1692.84 120.0)
          (within? (get-in cmp [:upgrade :simulated :gamma-h-kj-kg]) 1427.82 80.0))}))

(defn format-result [{:keys [mode q-in-kj q-out-kj cop-h gamma-h-kj-kg cop-c]}]
  (str (name mode)
       "  Qin=" (format "%.0f" q-in-kj) " kJ"
       "  Qout=" (format "%.0f" q-out-kj) " kJ"
       "  COPh=" (format "%.3f" cop-h)
       "  γh=" (format "%.0f" gamma-h-kj-kg) " kJ/kg"
       (when cop-c (str "  COPc=" (format "%.3f" cop-c)))))

(defn run-default []
  (let [results (reactor/make-system {:x 0.85 :mu 8})]
    {:results results
     :validation (validation-report)
     :lines (map format-result (vals results))}))
