(ns mithril.checkpoint-ipq
  "Bounded IPQ-style CAR transport for a Mithril CID checkpoint history.

  CAR replay proves the selected bytes and their CID links. Mithril's own
  ontology and causal verifier remains mandatory on import. This local adapter
  does not claim a remote IPQ service or a distributed mutable ref."
  (:require [ipld.car.trustless :as trustless]
            [mithril.checkpoint-ipld :as checkpoint-ipld]
            [mithril.checkpoint-ipld-fs :as local]))

(def history-selector
  {:selector :explore-recursive
   :limit {:mode :depth :depth 32}
   :sequence {:selector :explore-fields
              :fields {"parents" {:selector :explore-all
                                   :next {:selector :explore-recursive-edge}}}}})

(def limits
  ;; Each parent hop crosses a map field and a list index, so a 32-block
  ;; history needs up to 62 selector path components, not 32.
  {:max-blocks 32 :max-bytes 4194304 :max-depth 64 :max-matches 32})

(def max-car-bytes (+ (:max-bytes limits) 65536))

(defn- refuse! [reason]
  (throw (ex-info "Mithril checkpoint CAR refused"
                  {:mithril/error :mithril.checkpoint-ipq/refused
                   :reason reason})))

(defn export-history!
  "Export a fully checked, immutable history rooted at head as a bounded CAR.
  The selector must touch exactly the nodes the semantic verifier reached."
  [store schema head]
  (let [history (checkpoint-ipld/verify-history-nodes!
                 #(local/get-block store %) schema head)]
    (when (> (:blocks history) (:max-blocks limits))
      (refuse! :history-over-ipq-limit))
    (let [selected (trustless/selection-car
                    #(local/get-block store %) head history-selector limits)
          expected (set (keys (:nodes history)))
          actual (set (map :cid (:blocks selected)))]
      (when-not (= expected actual)
        (refuse! :selector-incomplete-history))
      (let [bytes (get-in selected [:car :bytes])]
        (when (> (.-length bytes) max-car-bytes)
          (refuse! :car-over-limit))
        {:root head :blocks (:blocks history) :bytes bytes}))))

(defn verify-history-car!
  "Replay a CAR against the caller's root, then rerun Mithril's complete
  ontology, SHACL, graph-digest and causal-history verification using only
  replayed bytes. No filesystem or archive-declared root is trusted."
  [schema expected-root car-bytes]
  (when-not (instance? js/Uint8Array car-bytes)
    (refuse! :invalid-car-bytes))
  (when (> (.-length car-bytes) max-car-bytes)
    (refuse! :car-over-limit))
  (let [replayed (trustless/verify-selection-car
                  car-bytes expected-root history-selector limits)
        loaded (:loaded replayed)]
    (when (seq (:unused replayed))
      (refuse! :unused-car-blocks))
    (let [blocks (into {} (map (juxt :cid :bytes)) loaded)
          history (checkpoint-ipld/verify-history-nodes!
                   #(get blocks %) schema expected-root)]
      (when-not (= (set (keys blocks)) (set (keys (:nodes history))))
        (refuse! :selector-incomplete-history))
      {:root expected-root :blocks (:blocks history)
       :state (get-in history [:nodes expected-root :state])
       :loaded loaded
       :semantic-graph-digest
       (get-in history [:nodes expected-root :semantic-graph-digest])})))

(defn import-history!
  "Verify a CAR against the caller's expected root, persist only its reached
  CID-checked blocks, then compare every stored byte with the CAR-verified
  bytes before publishing a new ref. The CAR's complete semantic proof is
  reused within this call; later verifications still re-read the store.
  An existing name is never changed. Partial block writes
  before a failure are harmless immutable data with no published ref."
  [store schema name expected-root car-bytes]
  (when (local/ref-exists? store name) (refuse! :ref-exists))
  (let [ontology-contract (checkpoint-ipld/ontology-source-contract)
        {:keys [loaded] :as checked}
        (verify-history-car! schema expected-root car-bytes)]
    (doseq [{:keys [cid bytes]} loaded]
      (local/put-block! store cid bytes))
    (local/create-byte-matched-ref!
     store name expected-root loaded ontology-contract)
    (dissoc checked :loaded)))
