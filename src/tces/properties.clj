(ns tces.properties
  "NiCl2–SrCl2/NH3 resorption heat transformer (Yan et al., Appl. Therm. Eng. 167, 2020).
   Thermophysical data are not tabulated in the paper; defaults follow the same
   equation framework as Li et al. (Int. J. Heat Mass Transfer, 2014) and are
   calibrated to reproduce Table 1 / Fig. 6–7 at X = 0.85 and μ = 8.")

(def default-cycle
  {:n-cya-mol     100.0    ; 0.1 kmol ammonia per Table 2
   :m-fprs-kg     5.795    ; NiCl2·6NH3 (forward / charged HTR)
   :m-rprs-kg     4.091    ; NiCl2·2NH3  (reverse / discharged HTR)
   :m-fsrs-kg     4.211    ; SrCl2·7NH3  (forward / charged LTR)
   :m-rsrs-kg     2.508    ; SrCl2·NH3   (reverse / discharged LTR)
   :m-prs-kg      3.24     ; anhydrous NiCl2 basis for γ_h (Eq. 19)
   :dh-prs-kj-mol 60.0     ; per mol NH3 in cycle (Eq. 4)
   :dh-srs-kj-mol 28.0     ; per mol NH3 in cycle (Eq. 10)
   :cp-fprs       1.026    ; kJ·kg⁻¹·°C⁻¹
   :cp-rprs       0.884
   :cp-fsrs       1.15
   :cp-rsrs       0.95
   :cp-metal      0.5      ; HTR / LTR structural metal
   :theta-c       5.0})   ; heat-exchange temperature lift (Section 4.1)

(def default-salts
  {:prs {:dh-kj-mol 60.0 :ds-kj-mol-k 0.108}
   :srs {:dh-kj-mol 28.0 :ds-kj-mol-k 0.095}})

(def mode-temperatures
  {:direct   {:t-amb 25 :t0 156 :t-ad-l 30 :t-de-h 163 :t-reg 20 :t-ad-h 149
              :t-in 168 :t-out 144 :t-charge-start 144}
   :upgrade  {:t-amb 25 :t0 156 :t-ad-l 30 :t-de-h 163 :t-reg 50 :t-ad-h 192
              :t-in 168 :t-out 187 :t-charge-start 144}
   :combined {:t-amb 25 :t0 156 :t-ad-l 30 :t-de-h 163 :t-reg -30 :t-ad-h 76
              :t-in 168 :t-out 71 :t-charge-start 25}})

(def paper-benchmarks
  {:x 0.85 :mu 8
   :direct   {:cop-h 0.973 :gamma-h-kj-kg 1692.84}
   :upgrade  {:cop-h 0.550 :gamma-h-kj-kg 1427.82}
   :combined {:cop-h 0.902 :gamma-h-kj-kg 2099.84 :cop-c 0.361}})
