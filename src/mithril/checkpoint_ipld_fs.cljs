(ns mithril.checkpoint-ipld-fs
  "Private local block/ref adapter for the verified Mithril IPLD value layer.
  Ref updates are serialized and atomic on one filesystem. This does not
  claim distributed CAS, publication, or crash-durable fsync semantics."
  (:require [clojure.string :as str]
            [ipld.core :as ipld]
            [mithril.checkpoint-ipld :as checkpoint-ipld]
            [multiformats.core :as multiformats]))

(def fs (js/require "node:fs"))
(def path (js/require "node:path"))
(def crypto (js/require "node:crypto"))

;; This adapter is Node-only. Prove the host digest against the portable
;; implementation before using it for either CID creation or verification.
(defonce ^:private installed-sha256
  (multiformats/install-sha256!
   (fn [bytes]
     (-> (.createHash crypto "sha256")
         (.update (if (instance? js/Uint8Array bytes)
                    bytes
                    (js/Uint8Array.from (into-array bytes))))
         (.digest)))))

(defn- refuse! [reason]
  (throw (ex-info "Mithril local IPLD store refused"
                  {:mithril/error :mithril.checkpoint-ipld-fs/refused
                   :reason reason})))

(defn open! [directory]
  (when-not (and (string? directory) (.isAbsolute path directory))
    (refuse! :invalid-directory))
  (.mkdirSync fs (.join path directory "blocks") #js {:recursive true :mode 448})
  (.mkdirSync fs (.join path directory "refs") #js {:recursive true :mode 448})
  {:directory directory})

(defn- block-path [store id]
  (when-not (and (string? id) (re-matches #"b[a-z2-7]+" id))
    (refuse! :invalid-cid))
  (.join path (:directory store) "blocks" id))

(defn- ref-path [store name]
  (when-not (and (string? name) (re-matches #"[A-Za-z0-9][A-Za-z0-9._-]*" name))
    (refuse! :invalid-ref-name))
  (.join path (:directory store) "refs" name))

(defn get-block [store id]
  (let [file (block-path store id)]
    (when (.existsSync fs file) (.readFileSync fs file))))

(defn put-block! [store id bytes]
  (when-not (= id (ipld/cid bytes)) (refuse! :block-cid-mismatch))
  (let [file (block-path store id)]
    (if (.existsSync fs file)
      (let [old (ipld/get-verified-block #(get-block store %) id)]
        (when-not (.equals old bytes)
          (refuse! :block-conflict)))
      (try (.writeFileSync fs file bytes #js {:flag "wx" :mode 384})
           (catch :default error
             (if (= "EEXIST" (.-code error))
               (let [old (ipld/get-verified-block #(get-block store %) id)]
                 (when-not (.equals old bytes)
                   (refuse! :block-conflict)))
               (throw error)))))
    id))

(defn- with-write-lock [store f]
  (let [file (.join path (:directory store) ".write.lock")
        fd (try (.openSync fs file "wx" 384)
                (catch :default _ (refuse! :store-locked)))]
    (try
      (.writeFileSync fs fd (str (.-pid js/process)) "utf8")
      (f)
      (finally
        (.closeSync fs fd)
        (.unlinkSync fs file)))))

(defn with-store-lock! [store f]
  "Serialize a small block/ref/receipt transaction on this one filesystem.
  This is not a distributed lease or an fsync durability guarantee."
  (with-write-lock store f))

(defn read-ref [store name]
  (let [file (ref-path store name)]
    (when-not (.existsSync fs file) (refuse! :missing-ref))
    (let [id (str/trim (.readFileSync fs file "utf8"))]
      (when-not (re-matches #"b[a-z2-7]+" id) (refuse! :invalid-ref))
      id)))

(defn ref-exists? [store name]
  (.existsSync fs (ref-path store name)))

(defn- create-ref-under-lock! [store name id]
  (when-not (re-matches #"b[a-z2-7]+" (str id)) (refuse! :invalid-cid))
  (try (.writeFileSync fs (ref-path store name) (str id "\n")
                       #js {:flag "wx" :mode 384})
       (catch :default error
         (if (= "EEXIST" (.-code error)) (refuse! :ref-exists)
             (throw error))))
  id)

(defn- advance-ref-under-lock! [store name expected next-id]
  (when-not (= expected (read-ref store name)) (refuse! :ref-conflict))
  (let [file (ref-path store name)
        temporary (str file ".tmp-" (.-pid js/process) "-" (.now js/Date))]
    (.writeFileSync fs temporary (str next-id "\n")
                    #js {:flag "wx" :mode 384})
    (.renameSync fs temporary file))
  next-id)

(defn import-state! [store schema name state]
  (with-write-lock store
    (fn []
      (when (.existsSync fs (ref-path store name)) (refuse! :ref-exists))
      (let [id (checkpoint-ipld/put!
                #(put-block! store %1 %2) #(get-block store %) schema state [])]
        (create-ref-under-lock! store name id)))))

(defn record-state!
  "Append one checked state to an exact local ref head. The expected head is
  supplied by the caller's prior-state verification; no missing ref is silently
  created for an already-running session."
  [store schema name expected state]
  (with-write-lock store
    (fn []
      (let [exists? (.existsSync fs (ref-path store name))
            observed (when exists? (read-ref store name))]
        (when-not (= expected observed) (refuse! :ref-conflict))
        (let [id (checkpoint-ipld/put!
                  #(put-block! store %1 %2) #(get-block store %) schema
                  state (if observed [observed] []))]
          (if observed
            (advance-ref-under-lock! store name observed id)
            (create-ref-under-lock! store name id)))))))

(defn fork-ref! [store source name]
  (with-write-lock store
    (fn [] (create-ref-under-lock! store name (read-ref store source)))))

(defn advance-ref! [store schema name expected next-id]
  (with-write-lock store
    (fn []
      (let [saved (checkpoint-ipld/read! #(get-block store %) schema next-id)]
        (when-not (or (= expected next-id)
                      (some #{expected} (:parents saved)))
          (refuse! :noncausal-ref-advance))
        (advance-ref-under-lock! store name expected next-id)))))

(defn advance-fact! [store schema name fact]
  (when-not (keyword? fact) (refuse! :invalid-fact))
  (with-write-lock store
    (fn []
      (let [head (read-ref store name)
            state (:state (checkpoint-ipld/read! #(get-block store %) schema head))]
        (when-not (and (= :running (:status state)) (nil? (:awaiting state)))
          (refuse! :effect-or-terminal-state))
        (if (contains? (:facts state) fact)
          head
          (let [next-id (checkpoint-ipld/put!
                         #(put-block! store %1 %2) #(get-block store %) schema
                         (update state :facts conj fact) [head])]
            (advance-ref-under-lock! store name head next-id)))))))

(defn merge-refs! [store schema base left right output]
  (with-write-lock store
    (fn []
      (when (.existsSync fs (ref-path store output)) (refuse! :ref-exists))
      (let [id (checkpoint-ipld/merge-facts!
                #(put-block! store %1 %2) #(get-block store %) schema
                (read-ref store base) (read-ref store left) (read-ref store right))]
        (create-ref-under-lock! store output id)))))

(defn verify-ref! [store schema name]
  (checkpoint-ipld/verify-history! #(get-block store %) schema (read-ref store name)))

(defn verify-ref-nodes! [store schema name]
  (checkpoint-ipld/verify-history-nodes!
   #(get-block store %) schema (read-ref store name)))

(defn create-verified-ref!
  "Publish a previously imported immutable head under a new local name only
  after rechecking every stored block and causal transition. No existing ref
  is advanced or overwritten. The check and publication share the ref lock."
  [store schema name head]
  (with-write-lock store
    (fn []
      (when (.existsSync fs (ref-path store name)) (refuse! :ref-exists))
      (checkpoint-ipld/verify-history! #(get-block store %) schema head)
      (create-ref-under-lock! store name head))))

(defn upgrade-ref-v7!
  "Re-encode a verified causal history as v7 under a new ref. The source ref
  remains untouched; every parent transition is checked again before publish."
  [store schema source target]
  (when (ref-exists? store target) (refuse! :ref-exists))
  (let [old-head (read-ref store source)
        old-history (checkpoint-ipld/verify-history!
                     #(get-block store %) schema old-head)
        mapped (atom {})]
    (letfn [(recode [id]
              (if-let [known (get @mapped id)]
                known
                (let [{:keys [state parents]}
                      (checkpoint-ipld/read! #(get-block store %) schema id)
                      next-parents (vec (sort (map recode parents)))
                      next-id (checkpoint-ipld/put!
                               #(put-block! store %1 %2)
                               #(get-block store %) schema state next-parents)]
                  (swap! mapped assoc id next-id)
                  next-id)))]
      (let [new-head (recode old-head)
            new-history (checkpoint-ipld/verify-history!
                         #(get-block store %) schema new-head)]
        (when-not (and (= (:blocks old-history) (:blocks new-history))
                       (= (:state (checkpoint-ipld/read!
                                   #(get-block store %) schema old-head))
                          (:state (checkpoint-ipld/read!
                                   #(get-block store %) schema new-head))))
          (refuse! :upgrade-history-mismatch))
        (with-write-lock store
          (fn []
            (when-not (= old-head (read-ref store source))
              (refuse! :ref-conflict))
            (create-ref-under-lock! store target new-head)))
        {:source source :target target :old-head old-head
         :new-head new-head :blocks (:blocks new-history)}))))
