(ns tces.modes
  (:require [tces.properties :as props]
            [tces.thermo :as th]))

(defn- metal-mass [mu salt-kg] (* mu salt-kg))

(defn- cycle+temps [cycle mode-key]
  (merge cycle (props/mode-temperatures mode-key)))

(defn direct-charge-qin
  ([c] (direct-charge-qin c :direct))
  ([{:keys [x mu] :as c} mode-key]
  (let [{:keys [n-cya-mol m-fprs-kg dh-prs-kj-mol cp-fprs cp-rprs cp-metal
                t-charge-start t-de-h t-in]} (cycle+temps c mode-key)
        m-htr (metal-mass mu m-fprs-kg)]
    {:q-r-prs (th/reaction-heat x n-cya-mol dh-prs-kj-mol)
     :q-s-prs (th/partition-prs-charge
               {:cp-fprs cp-fprs :cp-rprs cp-rprs :m-fprs-kg m-fprs-kg :x x
                :t-start t-charge-start :t-de-h t-de-h :t-in t-in})
     :q-s-htr (th/sensible cp-metal m-htr t-charge-start t-in)})))

(defn direct-discharge-qout
  ([c] (direct-discharge-qout c :direct))
  ([{:keys [x mu] :as c} mode-key]
  (let [{:keys [n-cya-mol m-fprs-kg m-rprs-kg dh-prs-kj-mol cp-fprs cp-rprs cp-metal
                t-in t-ad-h t-out]} (cycle+temps c mode-key)
        m-htr (metal-mass mu m-fprs-kg)
        q-r (th/reaction-heat x n-cya-mol dh-prs-kj-mol)
        q-s-prs (th/partition-prs-discharge-direct
                 {:cp-fprs cp-fprs :cp-rprs cp-rprs
                  :m-fprs-kg m-fprs-kg :m-rprs-kg m-rprs-kg :x x
                  :t-in t-in :t-ad-h t-ad-h :t-out t-out})
        q-s-htr (th/sensible cp-metal m-htr t-in t-out)]
    (- q-r q-s-prs q-s-htr))))

(defn upgrade-charge-qin
  [{:keys [x mu] :as c}]
  (let [base (direct-charge-qin c)
        {:keys [n-cya-mol m-fsrs-kg dh-srs-kj-mol cp-fsrs cp-rsrs cp-metal
                  t-ad-l t-reg]} (cycle+temps c :upgrade)
        m-ltr (metal-mass mu m-fsrs-kg)]
    (merge base
           {:q-r-srs (th/reaction-heat x n-cya-mol dh-srs-kj-mol)
            :q-s-srs (th/partition-srs-charge-upgrade
                      {:cp-fsrs cp-fsrs :cp-rsrs cp-rsrs :m-fsrs-kg m-fsrs-kg
                       :x x :t-ad-l t-ad-l :t-reg t-reg})
            :q-s-ltr (th/sensible cp-metal m-ltr t-ad-l t-reg)})))

(defn upgrade-discharge-qout
  [{:keys [x mu] :as c}]
  (let [{:keys [n-cya-mol m-fprs-kg dh-prs-kj-mol cp-fprs cp-rprs cp-metal
                t-in t-out]} (cycle+temps c :upgrade)
        m-htr (metal-mass mu m-fprs-kg)
        q-r (th/reaction-heat x n-cya-mol dh-prs-kj-mol)
        q-s-prs (th/partition-prs-discharge-upgrade
                 {:cp-fprs cp-fprs :cp-rprs cp-rprs :m-fprs-kg m-fprs-kg :x x
                  :t-in t-in :t-out t-out})
        q-s-htr (th/sensible cp-metal m-htr t-in t-out)]
    (- q-r q-s-prs q-s-htr)))

(defn combined-charge-qin
  "Eq. (3) with combined-mode charge path (long-term storage uses lower t-charge-start)."
  [c]
  (direct-charge-qin c :combined))

(defn combined-discharge-qout
  [{:keys [x mu] :as c}]
  (let [{:keys [n-cya-mol m-fprs-kg m-rprs-kg dh-prs-kj-mol cp-fprs cp-rprs cp-metal
                t-in t-ad-h t-out]} (cycle+temps c :combined)
        m-htr (metal-mass mu m-fprs-kg)
        q-r (th/reaction-heat x n-cya-mol dh-prs-kj-mol)
        q-s-prs (th/partition-prs-discharge-direct
                 {:cp-fprs cp-fprs :cp-rprs cp-rprs
                  :m-fprs-kg m-fprs-kg :m-rprs-kg m-rprs-kg :x x
                  :t-in t-in :t-ad-h t-ad-h :t-out t-out})
        q-s-htr (th/sensible cp-metal m-htr t-in t-out)]
    (- q-r q-s-prs q-s-htr)))

(defn sum-qin [parts]
  (reduce + 0.0 (vals parts)))

(defn mode-result
  [mode-key cycle]
  (let [charge-fn (case mode-key
                    :direct   direct-charge-qin
                    :upgrade  upgrade-charge-qin
                    :combined combined-charge-qin)
        discharge-fn (case mode-key
                       :direct   direct-discharge-qout
                       :upgrade  upgrade-discharge-qout
                       :combined combined-discharge-qout)
        charge-parts (charge-fn cycle)
        q-in (sum-qin charge-parts)
        q-out (discharge-fn cycle)
        {:keys [m-prs-kg]} cycle
        cop-h (/ q-out q-in)
        gamma-h (/ q-out m-prs-kg)
        q-cold (when (= mode-key :combined)
                 (let [{:keys [x mu n-cya-mol m-fsrs-kg dh-srs-kj-mol cp-fsrs cp-rsrs cp-metal
                               t-reg t-ad-l]} (cycle+temps cycle :combined)]
                   (th/cold-production
                    {:cp-fsrs cp-fsrs :cp-rsrs cp-rsrs :cp-metal cp-metal
                     :m-fsrs-kg m-fsrs-kg :x x :n-cya-mol n-cya-mol
                     :dh-srs-kj-mol dh-srs-kj-mol :m-ltr-kg (metal-mass mu m-fsrs-kg)
                     :t-reg t-reg :t-ad-l t-ad-l})))]
    (cond-> {:mode mode-key
             :q-in-kj q-in
             :q-out-kj q-out
             :cop-h cop-h
             :gamma-h-kj-kg gamma-h
             :charge-parts charge-parts}
      q-cold (assoc :q-cold-kj q-cold
                    :cop-c (/ q-cold q-in)))))

(defn all-modes [cycle]
  (map #(mode-result % cycle) [:direct :upgrade :combined]))
