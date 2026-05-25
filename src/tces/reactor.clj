(ns tces.reactor
  "Two-reactor resorption heat transformer (HTR + LTR)."
  (:require [tces.properties :as props]
            [tces.modes :as modes]))

(defn make-system
  "Build a simulation state map.
   Options:
     :x         global conversion rate (0–1)
     :mu        metal-to-salt mass ratio per reactor
     :cycle     override default-cycle fields
     :mode      optional single mode keyword"
  [{:keys [x mu cycle mode]
    :or {x 0.85 mu 8}}]
  (let [base (merge props/default-cycle cycle)
        state (assoc base :x x :mu mu)]
    (if mode
      (modes/mode-result mode state)
      (into {} (map (juxt :mode identity) (modes/all-modes state))))))

(defn sweep-x
  [mu & {:keys [from to step cycle] :or {from 0.1 to 1.0 step 0.05}}]
  (for [x (range from (+ to step 1e-9) step)]
    (let [results (modes/all-modes (merge props/default-cycle cycle {:x x :mu mu}))]
      {:x x :modes (into {} (map (juxt :mode identity) results))})))

(defn sweep-mu
  [x & {:keys [from to step cycle] :or {from 1 to 12 step 1}}]
  (for [mu (range from (inc to) step)]
    (let [results (modes/all-modes (merge props/default-cycle cycle {:x x :mu mu}))]
      {:mu mu :modes (into {} (map (juxt :mode identity) results))})))

(defn htr-description [cycle]
  {:reactor :htr
   :salt "NiCl2/NH3"
   :forward "NiCl2·6NH3 → NiCl2·2NH3 + 4 NH3"
   :reverse "NiCl2·2NH3 + 4 NH3 → NiCl2·6NH3"
   :masses-kg {:charged (:m-fprs-kg cycle) :discharged (:m-rprs-kg cycle)}
   :metal-mass-kg (* (:mu cycle) (:m-fprs-kg cycle))})

(defn ltr-description [cycle]
  {:reactor :ltr
   :salt "SrCl2/NH3"
   :forward "SrCl2·NH3 + 7 NH3 → SrCl2·7NH3"
   :reverse "SrCl2·7NH3 → SrCl2·NH3 + 7 NH3"
   :masses-kg {:charged (:m-fsrs-kg cycle) :discharged (:m-rsrs-kg cycle)}
   :metal-mass-kg (* (:mu cycle) (:m-fsrs-kg cycle))})
