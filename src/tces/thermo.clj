(ns tces.thermo
  "Sensible and reaction heat terms (Yan et al. 2020, §3.2–3.3).")

(defn sensible
  "Sensible heat (kJ) for heating/cooling between t1-c and t2-c (°C)."
  [cp-kg-k mass-kg t1-c t2-c]
  (* cp-kg-k mass-kg (- t2-c t1-c)))

(defn reaction-heat [x n-cya-mol dh-kj-mol]
  (* x n-cya-mol dh-kj-mol))

(defn partition-prs-charge
  "Eq. (6) — direct / combined charging sensible heat on PRS."
  [{:keys [cp-fprs cp-rprs m-fprs-kg x t-start t-de-h t-in]}]
  (+ (sensible cp-fprs m-fprs-kg t-start t-de-h)
     (sensible cp-rprs (* x m-fprs-kg) t-de-h t-in)
     (sensible cp-fprs (* (- 1.0 x) m-fprs-kg) t-de-h t-in)))

(defn partition-prs-charge-upgrade
  "Eq. (9) — upgrade mode charging sensible heat on PRS."
  [{:keys [cp-fprs cp-rprs m-fprs-kg x t-start t-in]}]
  (+ (sensible cp-rprs (* x m-fprs-kg) t-start t-in)
     (sensible cp-fprs (* (- 1.0 x) m-fprs-kg) t-start t-in)))

(defn partition-prs-discharge-direct
  "Eq. (15) — direct / combined discharging sensible heat on PRS."
  [{:keys [cp-fprs cp-rprs m-fprs-kg m-rprs-kg x t-in t-ad-h t-out]}]
  (+ (sensible cp-rprs m-rprs-kg t-in t-ad-h)
     (sensible cp-fprs (* x m-fprs-kg) t-ad-h t-out)
     (sensible cp-rprs (* (- 1.0 x) m-fprs-kg) t-ad-h t-out)))

(defn partition-prs-discharge-upgrade
  "Eq. (16) — upgrade mode discharging sensible heat on PRS."
  [{:keys [cp-fprs cp-rprs m-fprs-kg x t-in t-out]}]
  (+ (sensible cp-fprs (* x m-fprs-kg) t-in t-out)
     (sensible cp-rprs (* (- 1.0 x) m-fprs-kg) t-in t-out)))

(defn partition-srs-charge-upgrade
  "Eq. (11) — upgrade mode SRS sensible heat during charging."
  [{:keys [cp-fsrs cp-rsrs m-fsrs-kg x t-ad-l t-reg]}]
  (+ (sensible cp-rsrs (* x m-fsrs-kg) t-ad-l t-reg)
     (sensible cp-fsrs (* (- 1.0 x) m-fsrs-kg) t-ad-l t-reg)))

(defn cold-production
  "Eq. (17) — combined cooling and heating mode cold output."
  [{:keys [cp-fsrs cp-rsrs cp-metal m-fsrs-kg x n-cya-mol dh-srs-kj-mol
           m-ltr-kg t-reg t-ad-l]}]
  (- (reaction-heat x n-cya-mol dh-srs-kj-mol)
     (sensible cp-rsrs (* x m-fsrs-kg) t-reg t-ad-l)
     (sensible cp-fsrs (* (- 1.0 x) m-fsrs-kg) t-reg t-ad-l)
     (sensible cp-metal m-ltr-kg t-reg t-ad-l)))

(defn clapeyron-t-eq-c
  "Equilibrium temperature from Clausius–Clapeyron (Eq. 2), °C.
   ln(P/Pa) = (-ΔH/R)(1/T_K) + ΔS/R  with ΔH in kJ/mol, ΔS in kJ·mol⁻¹·K⁻¹."
  [p-pa dh-kj-mol ds-kj-mol-k]
  (let [r-kj 8.314e-3
        inv-t (+ (/ ds-kj-mol-k r-kj) (/ (- dh-kj-mol) r-kj) (/ (Math/log p-pa)))]
    (- (/ 1.0 inv-t) 273.15)))

(defn driving-t-in [t-de-h theta] (+ t-de-h theta))
(defn driving-t-out [t-ad-h theta] (- t-ad-h theta))
