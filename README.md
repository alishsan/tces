# TCES — Thermochemical Energy Storage Simulator

Thermochemical resorption model for **surplus heat utilization** (including **SMR BOP** studies) and reproduction of Yan et al. (2020).

Clojure implementation of the **multi-mode NiCl₂–SrCl₂/NH₃ resorption heat transformer** from:

> Ting Yan, Z.H. Kuai, S.F. Wu, *Multi-mode solid–gas thermochemical resorption heat transformer using NiCl₂–SrCl₂/NH₃*, Applied Thermal Engineering **167** (2020) 114800.  
> DOI: [10.1016/j.applthermaleng.2019.114800](https://doi.org/10.1016/j.applthermaleng.2019.114800)

## What it models

Two coupled reactors:

| Reactor | Salt | Role |
|---------|------|------|
| **HTR** (high-T) | NiCl₂ / NH₃ | Primary reactive salt (PRS) — stores heat |
| **LTR** (low-T) | SrCl₂ / NH₃ | Secondary reactive salt (SRS) — regeneration |

Three **discharge** operating modes (§2.1, Fig. 2–5):

1. **`:direct`** — store and release at similar temperature (COPh ≈ 0.97)
2. **`:upgrade`** — upgrade stored heat to a higher output temperature
3. **`:combined`** — simultaneous heating and cooling (cold from SRS regeneration)

The code implements the **lumped thermodynamic balance** (Eqs. 3–19): reaction enthalpy plus piecewise sensible-heat integrals for salt phases and reactor metal, with global conversion rate `X` and metal mass ratio `μ`.

## Improvements over a spreadsheet-style reproduction

- Explicit **mode temperature maps** (Table 1) and **cyclic charge** initial temperature for PRS
- **`sweep-x` / `sweep-mu`** for Fig. 6–9 style parametric studies
- Optional **Clausius–Clapeyron** helper (`tces.thermo/clapeyron-t-eq-c`) for equilibrium temperature from ΔH, ΔS
- Structured **charge heat breakdown** (`:charge-parts`) for debugging

> **Note:** The 2020 paper does not publish a full Cp / ΔH table. Defaults in `tces.properties` are calibrated so **direct** and **upgrade** modes match published COPh and γh at `X = 0.85`, `μ = 8` within tolerance. **Combined** mode COPh/COPc depend on unstated charge-path details in the paper; results are reported but not used in the pass/fail gate.

## SMR example (40 MWe, PWR-class steam 280–320 °C)

Secondary steam is **too hot** for direct NiCl₂ charging (~168 °C). The model assumes a **steam → HTF (~175 °C) heat exchanger**, then TCES on the BOP branch.

```bash
lein run smr          # sizing & mode recommendation
```

See [docs/SMR-concept.md](docs/SMR-concept.md) and [examples/smr-40mwe-pwr.edn](examples/smr-40mwe-pwr.edn).

## Usage

Requires [Leiningen](https://leiningen.org/).

```bash
# Yan (2020) benchmark (X=0.85, μ=8)
lein run

# 40 MWe SMR surplus-heat scenario
lein run smr

# Tests
lein test
```

### From the REPL

```clojure
(require '[tces.reactor :as r]
         '[tces.modes :as m])

;; Single mode
(m/mode-result :upgrade {:x 0.85 :mu 8})

;; All three modes
(r/make-system {:x 0.85 :mu 8})

;; Sweep conversion rate (Fig. 6)
(r/sweep-x 8 :from 0.1 :to 1.0 :step 0.05)
```

## Project layout

```
src/tces/
  properties.clj   # NiCl2–SrCl2/NH3 cycle data & paper benchmarks
  thermo.clj         # Sensible/reaction heat & Clapeyron
  modes.clj          # Qin/Qout per operating mode
  reactor.clj        # System builder & sweeps
  simulate.clj       # Validation vs. paper
  smr.clj            # 40 MWe PWR-class BOP scenarios
  main.clj           # CLI (lein run | lein run smr)
docs/
  SMR-concept.md     # Proposal narrative
examples/
  smr-40mwe-pwr.edn  # Editable scenario parameters
```

## References

- Yan et al. (2020) — primary model
- Li et al. (2014) *Int. J. Heat Mass Transfer* — related MgCl₂/NH₃ transformer equations
- Yan et al. (2019) *Appl. Therm. Eng.* — MnCl₂–SrCl₂/NH₃ composite cycle (same author group)
