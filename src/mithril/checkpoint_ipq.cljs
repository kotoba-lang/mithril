(ns mithril.checkpoint-ipq
  "Bounded IPQ-style CAR transport for a Mithril CID checkpoint history.

  CAR replay proves the selected bytes and their CID links. Mithril's own
  ontology and causal verifier remains mandatory on import. This local adapter
  does not claim a remote IPQ service or a distributed mutable ref."
  (:require [ipld.car.trustless :as trustless]
            [ipld.selector :as selector]
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
(def ipq-car-content-type "application/vnd.ipld.car")

(defn- refuse! [reason]
  (throw (ex-info "Mithril checkpoint CAR refused"
                  {:mithril/error :mithril.checkpoint-ipq/refused
                   :reason reason})))

(defn export-history!
  "Export a fully checked, immutable history rooted at head as a bounded CAR.
  The selector must touch exactly the nodes the semantic verifier reached.
  An optional process-local CID cache avoids rechecking unchanged node
  semantics; selection still rereads and verifies the complete CAR."
  ([store schema head]
   (export-history! store schema head nil))
  ([store schema head cache]
   (let [get-fn #(local/get-block store %)
         history (if cache
                   (checkpoint-ipld/verify-history-nodes-cached!
                    cache get-fn schema head)
                   (checkpoint-ipld/verify-history-nodes!
                    get-fn schema head))]
     (when (> (:blocks history) (:max-blocks limits))
       (refuse! :history-over-ipq-limit))
     (let [selected (trustless/selection-car
                     get-fn head history-selector limits)
           expected (set (keys (:nodes history)))
           actual (set (map :cid (:blocks selected)))]
       (when-not (= expected actual)
         (refuse! :selector-incomplete-history))
       (let [bytes (get-in selected [:car :bytes])]
         (when (> (.-length bytes) max-car-bytes)
           (refuse! :car-over-limit))
         {:root head :blocks (:blocks history) :bytes bytes})))))

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

;; Reinitializing this module invalidates every prior proof token. In
;; particular, a hot reload of verifier code must not retain old proofs.
(def ^:private proof-caches (js/WeakMap.))

(defn verification-cache
  "One-entry, process-local cache for proofs over exact immutable CAR bytes.
  It never certifies mutable local ref or block storage."
  []
  (let [token (js/Object.)]
    (.set proof-caches token nil)
    token))

(defn- copy-proof [proof]
  (update proof :loaded
          (fn [blocks]
            (mapv (fn [{:keys [bytes] :as block}]
                    (assoc block :bytes (.from js/Buffer bytes)))
                  blocks))))

(defn verify-history-car-cached!
  "Reuse a semantic proof only for byte-identical CAR input under the same
  root, schema and local ontology sources. A hit still compares all CAR bytes;
  importing the proof always rechecks the destination blocks under its lock."
  [cache schema expected-root car-bytes]
  (when-not (.has proof-caches cache)
    (refuse! :invalid-verification-cache))
  (when-not (instance? js/Uint8Array car-bytes)
    (refuse! :invalid-car-bytes))
  (when (> (.-length car-bytes) max-car-bytes)
    (refuse! :car-over-limit))
  (let [ontology-contract (checkpoint-ipld/ontology-source-contract)
        {:keys [root checked-schema sources car proof]} (.get proof-caches cache)]
    (if (and (= root expected-root)
             (= checked-schema schema)
             (= sources ontology-contract)
             (some? car)
             (.equals car car-bytes))
      (copy-proof proof)
      (let [checked (verify-history-car! schema expected-root car-bytes)]
        (when-not (= ontology-contract
                     (checkpoint-ipld/ontology-source-contract))
          (refuse! :ontology-contract-changed))
        (.set proof-caches cache {:root expected-root
                                 :checked-schema schema
                                 :sources ontology-contract
                                 :car (.from js/Buffer car-bytes)
                                 :proof (copy-proof checked)})
        checked))))

(defn import-history!
  "Verify a CAR against the caller's expected root, persist only its reached
  CID-checked blocks, then compare every stored byte with the CAR-verified
  bytes before publishing a new ref. The CAR's complete semantic proof is
  reused within this call; later verifications still re-read the store.
  An existing name is never changed. Partial block writes
  before a failure are harmless immutable data with no published ref."
  ([store schema name expected-root car-bytes]
   (import-history! store schema name expected-root car-bytes nil))
  ([store schema name expected-root car-bytes cache]
   (when (local/ref-exists? store name) (refuse! :ref-exists))
   (let [ontology-contract (checkpoint-ipld/ontology-source-contract)
         {:keys [loaded] :as checked}
         (if cache
           (verify-history-car-cached! cache schema expected-root car-bytes)
           (verify-history-car! schema expected-root car-bytes))]
     (doseq [{:keys [cid bytes]} loaded]
       (local/put-block! store cid bytes))
     (local/create-byte-matched-ref!
      store name expected-root loaded ontology-contract)
     (dissoc checked :loaded))))

(defn selection-url
  "Build an IPQ/1 request from an explicit endpoint origin and expected head.
  HTTP is allowed only for loopback experiments; production origins use HTTPS."
  [origin expected-root]
  (when-not (and (string? expected-root)
                 (re-matches #"b[a-z2-7]+" expected-root))
    (refuse! :invalid-cid))
  (let [url (try (js/URL. origin)
                 (catch :default _ (refuse! :invalid-ipq-origin)))
        hostname (.-hostname url)]
    (when-not (and (empty? (.-username url))
                   (empty? (.-password url))
                   (= "/" (.-pathname url))
                   (empty? (.-search url))
                   (empty? (.-hash url))
                   (or (= "https:" (.-protocol url))
                       (and (= "http:" (.-protocol url))
                            (contains? #{"localhost" "127.0.0.1" "[::1]"}
                                       hostname))))
      (refuse! :invalid-ipq-origin))
    (str (.-origin url) "/ipq/v1/selection/" expected-root
         "?selector=" (.toString (.from js/Buffer (selector/encode history-selector))
                                "base64url"))))

(defn fetch-history-car!
  "Fetch a bounded IPQ/1 CAR. Headers are hints; the streaming byte ceiling
  and trustless CAR replay are authoritative. Redirects are never followed."
  ([origin expected-root]
   (fetch-history-car! js/fetch origin expected-root))
  ([fetch-fn origin expected-root]
   (let [url (selection-url origin expected-root)]
     (-> (fetch-fn url #js {:redirect "error"
                           :signal (.timeout js/AbortSignal 120000)
                           :headers #js {"accept" ipq-car-content-type}})
         (.then
          (fn [response]
            (when-not (= 200 (.-status response))
              (refuse! :ipq-http-status))
            (let [headers (.-headers response)
                  content-type (.get headers "content-type")
                  profile (.get headers "x-ipq-profile")
                  length-header (.get headers "content-length")
                  length-number (when length-header (js/Number length-header))]
              (when-not (= "1" profile) (refuse! :ipq-profile-mismatch))
              (when-not (and (string? content-type)
                             (re-matches #"(?i)application/vnd\.ipld\.car(?:\s*;.*)?"
                                         content-type))
                (refuse! :ipq-content-type))
              (when (and length-header
                         (or (not (js/Number.isSafeInteger length-number))
                             (neg? length-number)
                             (> length-number max-car-bytes)))
                (refuse! :car-over-limit))
              (when-not (some? (.-body response))
                (refuse! :ipq-missing-body))
              (let [reader (.getReader (.-body response))]
                (letfn [(receive [chunks size]
                          (-> (.read reader)
                              (.then (fn [part]
                                       (if (.-done part)
                                         (js/Uint8Array.
                                          (.concat js/Buffer (into-array chunks) size))
                                         (let [bytes (.-value part)
                                               next-size (+ size (.-byteLength bytes))]
                                           (when (> next-size max-car-bytes)
                                             (.cancel reader)
                                             (refuse! :car-over-limit))
                                           (receive (conj chunks (.from js/Buffer bytes))
                                                    next-size)))))))]
                  (receive [] 0))))))
         (.catch (fn [error]
                   (let [cause (or (ex-cause error) error)
                         data (ex-data cause)]
                     (if (or (:reason data) (:type data))
                       (throw cause)
                       (refuse! :ipq-fetch-failed)))))))))

(defn fetch-import-history!
  "Fetch by caller-supplied head, replay the CAR, check ontology and causality,
  then publish a local ref only through import-history!'s guarded path."
  ([store schema name origin expected-root]
   (fetch-import-history! js/fetch store schema name origin expected-root nil))
  ([fetch-fn store schema name origin expected-root]
   (fetch-import-history! fetch-fn store schema name origin expected-root nil))
  ([fetch-fn store schema name origin expected-root cache]
   (-> (fetch-history-car! fetch-fn origin expected-root)
       (.then (fn [car-bytes]
                (import-history! store schema name expected-root car-bytes cache))))))
