(ns tces.core
  "Public API for the TCES resorption heat transformer simulator.
   Prefer this namespace in application code; sub-namespaces hold internals."
  (:require [tces.modes :as modes]
            [tces.properties :as properties]
            [tces.reactor :as reactor]
            [tces.simulate :as simulate]
            [tces.smr :as smr]))

;;; Defaults & reference data (re-exported)
(def default-cycle properties/default-cycle)
(def default-salts properties/default-salts)
(def mode-temperatures properties/mode-temperatures)
(def paper-benchmarks properties/paper-benchmarks)
(def default-40mwe-pwr smr/default-40mwe-pwr)
(def nicl2-srcl2-limits smr/nicl2-srcl2-limits)

;;; Thermodynamic cycle
(defn- cycle-state
  [{:keys [x mu cycle] :as state}]
  (merge default-cycle cycle (select-keys state [:x :mu])
         {:x (or x 0.85) :mu (or mu 8)}))

(defn mode-result
  "Performance of a single operating mode (`:direct`, `:upgrade`, `:combined`).
   `state` may include `:x`, `:mu`, and optional cycle overrides."
  [mode state]
  (modes/mode-result mode (cycle-state state)))

(defn simulate
  "Run all three modes. Same options as `make-system` without `:mode`."
  [opts]
  (reactor/make-system opts))

(defn make-system
  "Build simulation results. With `:mode`, returns one mode; otherwise a map of all three."
  [opts]
  (reactor/make-system opts))

(defn sweep-x
  "Sweep global conversion rate X at fixed μ (Fig. 6 style)."
  [mu & options]
  (apply reactor/sweep-x mu options))

(defn sweep-mu
  "Sweep metal mass ratio μ at fixed X (Fig. 8–9 style)."
  [x & options]
  (apply reactor/sweep-mu x options))

;;; Validation
(defn validate
  "Compare simulation to Yan et al. (2020) benchmarks at X=0.85, μ=8."
  []
  (simulate/validation-report))

(defn compare-to-paper [results]
  (simulate/compare-to-paper results))

;;; SMR integration
(defn smr-report
  "Full structured report for an SMR scenario map."
  [scenario]
  (smr/scenario-report scenario))

(defn recommend-mode
  "Pick `:direct`, `:upgrade`, or `:combined` for delivery requirements."
  [scenario]
  (smr/recommend-mode scenario))

(defn size-inventory
  "Scale salt inventory to a scenario's surplus heat window."
  [scenario]
  (smr/size-tces-inventory scenario))

(defn interface-feasibility
  "Check steam / HTF loop vs NiCl2–SrCl2/NH3 operating limits."
  [scenario]
  (smr/interface-feasibility scenario))

;;; Reactor descriptions
(defn htr-description [cycle] (reactor/htr-description cycle))
(defn ltr-description [cycle] (reactor/ltr-description cycle))
