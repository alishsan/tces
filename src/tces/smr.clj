(ns tces.smr
  "SMR surplus-heat integration scenarios (non-safety-class BOP).
   Reference plant: 40 MWe, PWR-class secondary steam 280–320 °C."
  (:require [tces.modes :as modes]
            [tces.properties :as props]
            [tces.thermo :as th]))

;;; ---------------------------------------------------------------------------
;;; Reference scenario — 40 MWe PWR-class SMR
;;; ---------------------------------------------------------------------------

(def default-40mwe-pwr
  {:name "40 MWe PWR-class SMR"
   :electric-power-mwe 40.0
   :thermal-efficiency 0.33          ; net η_el ≈ P_e / P_th (illustrative)
   :steam-t-min-c 280.0
   :steam-t-max-c 320.0
   :steam-pressure-mpa 7.0             ; ~saturation at 280 °C (order of magnitude)
   :interface-loop-t-c 175.0         ; proposed HTF after steam HX (not direct to salt)
   :surplus-fraction 0.15              ; share of P_th routed to TCES when heat is curtailed
   :surplus-hours 6.0
   :delivery-t-min-c 70.0
   :delivery-t-max-c 150.0
   :needs-cooling? false
   :x 0.85
   :mu 8})

(def nicl2-srcl2-limits
  "Operating band for Yan (2020) NiCl2–SrCl2/NH3 pair (Table 1, θ = 5 °C)."
  {:salt-pair "NiCl2-SrCl2/NH3"
   :t-charge-min-c 168                 ; Tin (driving desorption)
   :t-de-h-c 163
   :t-discharge-direct-c 144
   :t-discharge-upgrade-c 187
   :t-discharge-combined-c 71
   :theta-c (:theta-c props/default-cycle)})

(defn thermal-power-mwth
  "Derive thermal power (MW_th) from electric output and net efficiency."
  [{:keys [electric-power-mwe thermal-efficiency]}]
  (/ electric-power-mwe thermal-efficiency))

(defn surplus-thermal-power-mwth [scenario]
  (* (thermal-power-mwth scenario) (:surplus-fraction scenario)))

(defn surplus-energy-gj
  "Thermal energy available for storage over the surplus window (GJ_th)."
  [scenario]
  (* (surplus-thermal-power-mwth scenario)
     (:surplus-hours scenario)
     3.6))                                    ; MW·h → GJ (1 MWh = 3.6 GJ)

(def ^:private steam-table-c-mpa
  "Saturation pressure (MPa) vs temperature (°C) — steam-table anchor points."
  [[100 0.1013]
   [150 0.4758]
   [200 1.5537]
   [250 3.977]
   [280 6.406]
   [300 8.581]
   [320 11.053]
   [350 16.529]])

(defn saturated-steam-pressure-mpa
  "Interpolate saturation pressure (MPa) for water, 100–350 °C."
  [t-c]
  (let [pts steam-table-c-mpa
        below? (fn [row] (<= (first row) t-c))
        [t0 p0] (last (filter below? pts))
        above? (fn [row] (> (first row) t-c))
        [t1 p1] (first (filter above? pts))
        f (/ (- t-c t0) (- t1 t0))]
    (+ p0 (* f (- p1 p0)))))

(defn interface-feasibility
  "Check whether a BOP heat-transfer loop can charge the reference salt pair."
  [{:keys [interface-loop-t-c steam-t-min-c steam-t-max-c] :as scenario}
   & [{:keys [limits] :or {limits nicl2-srcl2-limits}}]]
  (let [{:keys [t-charge-min-c theta-c t-discharge-direct-c
                t-discharge-upgrade-c t-discharge-combined-c salt-pair]}
        limits
        t-min-charge (+ (:t-de-h-c limits) theta-c)
        steam-ok? (and (>= interface-loop-t-c t-min-charge)
                       (<= interface-loop-t-c steam-t-max-c))
        direct-ok? (<= (:delivery-t-max-c scenario) t-discharge-direct-c)
        upgrade-ok? (<= (:delivery-t-max-c scenario) t-discharge-upgrade-c)
        combined-ok? (and (:needs-cooling? scenario)
                          (<= (:delivery-t-min-c scenario) t-discharge-combined-c))]
    {:salt-pair salt-pair
     :interface-loop-t-c interface-loop-t-c
     :steam-header-c [steam-t-min-c steam-t-max-c]
     :t-charge-required-c t-min-charge
     :direct-coupling-to-steam? false
     :interface-feasible? steam-ok?
     :notes
     (cond
       (not steam-ok?)
       [(str "Interface loop must be ≥ " t-min-charge " °C to charge HTR (Tin), "
             "and below steam header. PWR steam at "
             steam-t-min-c "–" steam-t-max-c " °C requires a **temperature-stepping HX** "
             "(steam → HTF ~" interface-loop-t-c " °C), not direct salt–steam contact.")]
       :else
       [(str "Steam at " steam-t-min-c "–" steam-t-max-c " °C (~"
             (format "%.1f" (saturated-steam-pressure-mpa steam-t-min-c))
             "–" (format "%.1f" (saturated-steam-pressure-mpa steam-t-max-c))
             " MPa sat.) is stepped down to the TCES charge loop.")
        "Typical extraction points: feedwater heater / desuperheater / dedicated BOP branch."])
     :delivery-feasible?
     {:direct direct-ok? :upgrade upgrade-ok? :combined combined-ok?}}))

(defn recommend-mode
  [scenario & [feas]]
  (let [{:keys [delivery-feasible?]} (or feas (interface-feasibility scenario))
        {:keys [needs-cooling? delivery-t-max-c]} scenario
        {:keys [direct upgrade combined]} delivery-feasible?]
    (cond
      (and needs-cooling? combined) :combined
      (and (not direct) upgrade) :upgrade
      direct :direct
      upgrade :upgrade
      :else :upgrade)))

(defn cycle-performance
  "Thermodynamic KPIs from the lumped model for one reference inventory."
  [scenario]
  (let [cycle (merge props/default-cycle
                     (select-keys scenario [:x :mu]))
        mode (recommend-mode scenario)
        result (modes/mode-result mode cycle)]
    (assoc result
           :recommended-mode mode
           :inventory-scale (:m-prs-kg cycle))))

(defn size-tces-inventory
  "Scale reference salt inventory to absorb surplus energy over the scenario window."
  [scenario]
  (let [e-surplus-gj (surplus-energy-gj scenario)
        e-surplus-kj (* e-surplus-gj 1e6)
        {:keys [cop-h gamma-h-kj-kg recommended-mode q-in-kj q-out-kj]
         :as perf} (cycle-performance scenario)
        ref-m-prs (:m-prs-kg props/default-cycle)
        ;; Delivered heat ≈ surplus input × COPh (stored + sensible recovery)
        e-delivered-kj (* e-surplus-kj cop-h)
        m-prs-kg (/ e-delivered-kj gamma-h-kj-kg)
        scale (/ m-prs-kg ref-m-prs)
        ref-m-fprs (:m-fprs-kg props/default-cycle)
        ref-m-fsrs (:m-fsrs-kg props/default-cycle)]
    {:surplus-energy-gj e-surplus-gj
     :surplus-energy-mwh-th (/ e-surplus-gj 3.6)
     :cop-h cop-h
     :recommended-mode recommended-mode
     :delivered-energy-gj (* e-surplus-gj cop-h)
     :m-prs-kg m-prs-kg
     :m-htr-salt-kg (* scale ref-m-fprs)
     :m-ltr-salt-kg (* scale ref-m-fsrs)
     :inventory-scale scale
     :reference-cycle {:q-in-kj q-in-kj :q-out-kj q-out-kj :m-prs-kg ref-m-prs}}))

(defn scenario-report
  "Full structured report for proposals and CLI."
  [scenario]
  (let [feas (interface-feasibility scenario)
        sizing (size-tces-inventory scenario)
        perf (cycle-performance scenario)]
    {:scenario scenario
     :plant
     {:electric-power-mwe (:electric-power-mwe scenario)
      :thermal-power-mwth (thermal-power-mwth scenario)
      :surplus-power-mwth (surplus-thermal-power-mwth scenario)
      :steam-pressure-mpa-at-min-t (saturated-steam-pressure-mpa (:steam-t-min-c scenario))}
     :integration feas
     :thermodynamic perf
     :sizing sizing}))

(defn format-report-line
  [report]
  (let [{:keys [scenario plant integration sizing thermodynamic]} report
        mode (:recommended-mode sizing)]
    (str "=== " (:name scenario) " ===\n"
         "Plant: " (:electric-power-mwe scenario) " MWe → "
         (format "%.1f" (:thermal-power-mwth plant)) " MWth"
         "  |  surplus " (format "%.1f" (:surplus-power-mwth plant)) " MWth × "
         (:surplus-hours scenario) " h = "
         (format "%.0f" (:surplus-energy-mwh-th sizing)) " MWh_th ("
         (format "%.0f" (:surplus-energy-gj sizing)) " GJ)\n"
         "Steam header: " (:steam-t-min-c scenario) "–" (:steam-t-max-c scenario) " °C"
         "  (~" (format "%.1f" (:steam-pressure-mpa-at-min-t plant)) " MPa sat. at "
         (:steam-t-min-c scenario) " °C)\n"
         "TCES interface loop: " (:interface-loop-t-c scenario) " °C — "
         (if (:interface-feasible? integration) "OK for NiCl2 charge" "NOT OK") "\n"
         (str "  " (first (:notes integration)) "\n")
         "Mode: " (name mode)
         "  COPh=" (format "%.3f" (:cop-h sizing))
         "  γh=" (format "%.0f" (:gamma-h-kj-kg thermodynamic)) " kJ/kg (PRS basis)\n"
         "Sizing (one full discharge equivalent): NiCl2 inventory ~"
         (format "%.0f" (:m-prs-kg sizing)) " kg PRS basis"
         "  (HTR salt ~" (format "%.0f" (:m-htr-salt-kg sizing)) " kg"
         ", LTR salt ~" (format "%.0f" (:m-ltr-salt-kg sizing)) " kg"
         ", scale ×" (format "%.0f" (:inventory-scale sizing)) " vs lab reference)\n"
         "Delivered heat (ideal lumped): ~"
         (format "%.0f" (:delivered-energy-gj sizing)) " GJ over window\n")))
