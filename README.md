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

## Published Mithril code and libraries

The repository now contains source authored in Mithril itself:

- [`lib/web/v1.mith`](lib/web/v1.mith) is the first reusable Mithril library.
  It exports a closed exact router, static-response handler, text view and HTTP
  response effect as typed ontology symbols.
- [`examples/hello-web.mith`](examples/hello-web.mith) is a Mithril web
  application. It imports the library by its canonical RDF Dataset digest and
  defines `GET` and `HEAD` routes without generated source text.

`resources`, `lib` and `examples` are package classpath roots, so downstream
builds consume these exact files from the pinned Git commit instead of copying
or regenerating them.

The v1 web slice is deliberately small but executable. The compiler rejects a
changed library digest, a route that names anything except an imported handler,
unknown source keys, unsupported methods, malformed paths and duplicate route
identities. `mithril.web/dispatch` then executes the admitted Web IR without
ambient network authority. An HTTP host can map a typed request to this pure
boundary and write the returned response; network ingress remains a host
capability rather than hidden authority in the `.mith` program.

```text
lib/web/v1.mith (Library, JSON-LD)
  -> canonical RDF digest + typed export catalog
examples/hello-web.mith (WebApplication, JSON-LD)
  -> digest-pinned link + route admission
  -> :mithril.web/v1 IR
  -> deterministic request dispatch
```

This proves that a web framework can be expressed as ontology-linked Mithril
modules. It does not yet claim dynamic path parameters, middleware, streaming,
cookies, templates or a production HTTP listener; those require additional
typed library symbols and the Kotoba HTTP ingress capability qualification.

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
CP=src:../org-w3-json-ld-api/src:../org-w3-rdf-canon/src:../org-w3-nquads/src:../io-multiformats/src:../text/src:../org-nist-sha2/src
kbb --backend sci --classpath "$CP" bin/mithril.cljk compile examples/tender.mith
kbb --backend sci --classpath "$CP" bin/mithril.cljk compile-library lib/web/v1.mith
kbb --backend sci --classpath "$CP" bin/mithril.cljk compile-web examples/hello-web.mith lib/web/v1.mith
kbb --backend sci --classpath "$CP" bin/mithril.cljk request examples/hello-web.mith lib/web/v1.mith GET /hello
kbb --backend sci --classpath "$CP":test test/run.cljk
amu check src/mithril/runtime.kotoba --jvm-free
```

The CLI prints a JSON artifact containing the semantic graph digest and the
typed OaK transaction. The sibling `.mithril` example must compile to the same
semantic projection.
