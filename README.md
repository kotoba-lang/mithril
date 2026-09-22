# Mithril

The name is a metaphor: mithril is the imagined metal, used here for a source
surface whose graph semantics stay inspectable through compilation. It does
not name Tolkien's work or imply compatibility with another Mithril project.

Mithril is an ontology-based programming language whose implementation files
are JSON-LD. Both `.mith` and `.mithril` have exactly the same syntax and
semantics: JSON-LD 1.1 with media type `application/ld+json`.

Mithril is not a prose-to-code generator. A source document names a goal,
ontology identity and finite typed choices. The compiler expands it to an RDF
Dataset, canonicalizes that dataset, validates the closed Mithril vocabulary,
and emits an OaK transaction. OaK derives and revalidates Execution IR; Kotoba
and Amu provide the typed, capability-bounded runtime and Wasm compiler.

```text
program.mith / program.mithril (JSON-LD)
  -> pinned-context expansion
  -> canonical RDF Dataset digest
  -> Mithril vocabulary and shape admission
  -> :oak.transaction/v1
  -> OaK Execution IR
  -> Kotoba / Amu / Wasm
```

JSON-LD is the source notation, not the query engine, reasoner, SHACL engine or
runtime. Those responsibilities remain explicit: `org-w3-json-ld-api`,
`org-w3-rdf-canon`, OaK's OWL/SPARQL/SHACL adapters, and Kotoba/Amu.

## Source contract

- `.mith` and `.mithril` are aliases. The suffix never enters semantic identity.
- Remote contexts are never fetched. The v1 context is pinned by the compiler.
- Semantic identity is the SHA-256 digest of the canonical RDF Dataset, not the
  original JSON byte order or compacted spelling.
- Unknown keys, roles, duplicate roles, out-of-range confidence, and unpinned
  contexts fail closed before an OaK transaction is emitted.
- v1 composes a closed ontology catalog; it does not invent identifiers, text,
  algorithms, UI, or effects.

## Run

```sh
kbb --backend sci --classpath src:../org-w3-json-ld-api/src:../org-w3-rdf-canon/src:../org-w3-nquads/src:../io-multiformats/src bin/mithril.cljk compile examples/tender.mith
kbb --backend sci --classpath src:test:../org-w3-json-ld-api/src:../org-w3-rdf-canon/src:../org-w3-nquads/src:../io-multiformats/src test/run.cljk
amu check src/mithril/runtime.kotoba --jvm-free
```

The CLI prints a JSON artifact containing the semantic graph digest and the
typed OaK transaction. The sibling `.mithril` example must compile to the same
semantic projection.
