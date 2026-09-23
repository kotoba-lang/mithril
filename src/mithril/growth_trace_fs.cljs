(ns mithril.growth-trace-fs
  "One-filesystem append adapter for growth evidence. A completed line names
  the immutable IPLD block that contains its exact inputs and typed receipt."
  (:require [clojure.string :as str]
            [mithril.checkpoint-ipld-fs :as blocks]
            [mithril.growth-trace :as trace]))

(def fs (js/require "node:fs"))
(def path (js/require "node:path"))

(defn- refuse! [reason]
  (throw (ex-info "Mithril growth trace store refused"
                  {:mithril/error :mithril.growth-trace-fs/refused
                   :reason reason})))

(defn- lines [file]
  (if (.existsSync fs file)
    (let [value (.readFileSync fs file "utf8")]
      (when (and (seq value) (not (str/ends-with? value "\n")))
        (refuse! :partial-receipt-file))
      (vec (remove str/blank? (str/split-lines value))))
    []))

(defn- parse-line [line]
  (try (js->clj (js/JSON.parse line))
       (catch :default _ (refuse! :invalid-receipt-json))))

(defn- private-file! [file]
  (when (.existsSync fs file)
    (let [stat (.lstatSync fs file)]
      (when (or (.isSymbolicLink stat) (not (.isFile stat))
                (pos? (bit-and (.-mode stat) 63)))
        (refuse! :receipt-not-private)))))

(defn- private-store! [store]
  (let [stat (.lstatSync fs (:directory store))]
    (when (or (.isSymbolicLink stat) (not (.isDirectory stat))
              (pos? (bit-and (.-mode stat) 63)))
      (refuse! :store-not-private))))

(defn- verify-line! [store line]
  (let [record (parse-line line)
        id (get record "traceCid")]
    (when-not (string? id) (refuse! :missing-trace-cid))
    (let [saved (trace/read! #(blocks/get-block store %) id)
          expected (cond-> (assoc (trace/json-value (:receipt saved))
                                  "traceCid" id
                                  "definitionDigest" (:definition-digest saved))
                     (= trace/format-id (:format saved))
                     (assoc "semanticGraphDigest" (:semantic-graph-digest saved)
                            "semanticOntologyDigest" (:semantic-ontology-digest saved)))]
      (when-not (= record expected) (refuse! :receipt-block-mismatch))
      saved)))

(defn append!
  "A legacy untraced JSONL tail is refused; start a new traced ledger.
  Writers using this adapter serialize through the private store lock."
  [store receipt-file profile-path profile-source observation model-result receipt]
  (when-not (and (string? receipt-file) (.isAbsolute path receipt-file))
    (refuse! :invalid-receipt-path))
  (private-store! store)
  (blocks/with-store-lock! store
    (fn []
      (private-file! receipt-file)
      (let [prior-lines (lines receipt-file)
            prior (when-let [line (last prior-lines)] (verify-line! store line))
            history (when prior
                      (trace/verify-history! #(blocks/get-block store %) (:cid prior)))
            _ (when (contains? (:observation-ids history) (:id observation))
                (refuse! :duplicate-observation))
            parents (if prior [(:cid prior)] [])
            id (trace/put! #(blocks/put-block! store %1 %2)
                           #(blocks/get-block store %) profile-path profile-source
                           observation model-result receipt parents)
            saved (trace/read! #(blocks/get-block store %) id)
            full (assoc (trace/json-value receipt)
                        "traceCid" id
                        "definitionDigest" (:definition-digest saved)
                        "semanticGraphDigest" (:semantic-graph-digest saved)
                        "semanticOntologyDigest" (:semantic-ontology-digest saved))]
        (.mkdirSync fs (.dirname path receipt-file) #js {:recursive true :mode 448})
        (.appendFileSync fs receipt-file
                         (str (.stringify js/JSON (clj->js full)) "\n")
                         #js {:encoding "utf8" :mode 384})
        full))))

(defn verify-ledger! [store receipt-file current-profile-source]
  (private-store! store)
  (private-file! receipt-file)
  (let [entries (lines receipt-file)]
    (when (empty? entries) (refuse! :empty-ledger))
    (loop [entries entries prior nil count 0]
      (if (empty? entries)
        (let [history (trace/verify-history!
                       #(blocks/get-block store %) prior)]
          {:head prior :receipts count
           :blocks (:blocks history)
           :semantic-blocks (:semantic-blocks history)
           :legacy-blocks (:legacy-blocks history)})
        (let [saved (verify-line! store (first entries))]
          (when-not (= current-profile-source (:profile-source saved))
            (refuse! :profile-source-drift))
          (when-not (= (if prior [prior] []) (:parents saved))
            (refuse! :ledger-chain-mismatch))
          (recur (rest entries) (:cid saved) (inc count)))))))
