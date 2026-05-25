(ns tces.main
  (:require [tces.simulate :as sim])
  (:gen-class))

(defn -main [& _]
  (let [{:keys [lines validation]} (sim/run-default)
        cmp (:comparison validation)]
    (println "NiCl2–SrCl2/NH3 resorption heat transformer (Yan et al. 2020)")
    (println "Thermodynamic cycle at X = 0.85, μ = 8\n")
    (doseq [line lines] (println line))
    (println "\nValidation vs. paper (Appl. Therm. Eng. 167, 2020):")
    (doseq [mode [:direct :upgrade :combined]
            :let [{:keys [delta paper]} (cmp mode)]]
      (println (str "  " (name mode)
                    "  ΔCOPh=" (format "%+.3f" (:cop-h delta))
                    "  Δγh=" (format "%+.0f" (:gamma-h-kj-kg delta)) " kJ/kg"
                    "  (paper COPh=" (:cop-h paper) ")")))
    (println (if (:pass? validation)
               "\nBenchmarks: PASS (direct & upgrade within tolerance)"
               "\nBenchmarks: partial — see README"))))
