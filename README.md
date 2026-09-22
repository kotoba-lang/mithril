# Mithril

The name is a metaphor: mithril is the imagined metal, used here for a source
surface whose graph semantics stay inspectable through compilation. It does
not name Tolkien's work or imply compatibility with another Mithril project.

Mithril is an ontology-based programming language with a canonical,
Kotoba-shaped typed S-expression surface. Both `.mith` and `.mithril` are
aliases. New source uses `application/vnd.mithril.form`; the compiler also
accepts the existing JSON-LD 1.1 spelling (`application/ld+json`) without a
migration flag. Both lower to one pinned JSON-LD projection and therefore one
canonical RDF Dataset identity.

Mithril is not a prose-to-code generator. A source document names a goal,
ontology identity and finite typed choices. The compiler expands it to an RDF
Dataset, canonicalizes that dataset, validates the closed Mithril vocabulary,
and emits an OaK transaction. OaK derives and revalidates Execution IR; Kotoba
and Amu provide the typed, capability-bounded runtime and Wasm compiler.

```text
program.mith / program.mithril
  -> inert Mithril Form reader OR compatible JSON-LD reader
  -> one pinned JSON-LD projection
  -> canonical RDF Dataset digest
  -> Mithril vocabulary and shape admission
  -> :oak.transaction/v1
  -> OaK Execution IR
  -> Kotoba / Amu / Wasm
```

JSON-LD is the RDF interchange projection, not the query engine, reasoner,
SHACL engine or runtime. Those responsibilities remain explicit: `org-w3-json-ld-api`,
`org-w3-rdf-canon`, OaK's OWL/SPARQL/SHACL adapters, and Kotoba/Amu.

`mithril.execution/execute` is the bounded runtime prefix used by the app
compiler surface. It passes the asserted application graph through OaK's real
OWL 2 RL least-fixpoint adapter, executes the pinned SPARQL algebra engine over
the entailed graph, and validates the application node against OaK's canonical
SHACL shape document. The edge SHACL adapter names its supported
`minCount`/`maxCount`/datatype/`sh:in` subset in the receipt and rejects every
unknown constraint. Each step hashes its actual input and output; a violation
throws a named refusal and cannot appear as an executed step.

## Semantic core and execution core

Mithril does not make RDF the program-execution IR and does not put OWL inside
Osaho. The boundaries are:

```text
Mithril Form                    compact typed semantic/action syntax
  -> JSON-LD / RDF Dataset      facts, OWL 2 RL and SHACL interoperability
  -> typed semantic delta       assert / retract / query / infer / compile / stop
  -> deterministic executor     validates and applies the delta
  -> Kotoba HIR -> Osaho        checked executable semantics and DefCID
  -> Amu -> backend             weaving, lowering and verified artifacts
  -> IPLD / CID                 shared identity and physical value plane
```

Osaho remains the canonical checked executable KIR and definition-identity
contract. Amu remains the compiler/orchestrator. Mithril owns declarative world
semantics and their RDF projection. The two meet only at typed compile/call
actions and content identities.

The agent-facing contract is asymmetric by design: prefill is a bounded
semantic projection; decode is a smaller typed action/delta. A model never
rewrites the complete graph and never needs to emit prose. A Jev-like policy
may eventually select the operation and arguments directly; the same action
schema remains valid without a text decoder.

[`ontology/semantic-core-v1.mith`](ontology/semantic-core-v1.mith) demonstrates
native RDF, OWL 2 RL and SHACL forms. The compiler lowers it to JSON-LD and
canonicalizes the resulting RDF Dataset. [`examples/mithril-app-agent-form.mith`](examples/mithril-app-agent-form.mith)
is semantically identical to the compatibility JSON-LD app source and compiles
to the same graph digest and App IR.

## Published Mithril code and libraries

The repository now contains source authored in Mithril itself:

- [`lib/web/v1.mith`](lib/web/v1.mith) is the first reusable Mithril library.
  It exports a closed exact router, static-response handler, text view and HTTP
  response effect as typed ontology symbols.
- [`examples/hello-web.mith`](examples/hello-web.mith) is a Mithril web
  application. It imports the library by its canonical RDF Dataset digest and
  defines `GET` and `HEAD` routes without generated source text.
- [`examples/hello-web-synthesis.mith`](examples/hello-web-synthesis.mith)
  leaves only the handler choice open. OaK materializes the imported handler
  classes with OWL 2 RL, the SPARQL engine discovers the permitted finite
  candidates, and OpenJev selects one typed choice without generating source.
- [`examples/multiroute-web-synthesis.mith`](examples/multiroute-web-synthesis.mith)
  sends every route choice in one OpenJev request. The compiler checks one
  complete distribution per route, seals each admitted decision into Web IR,
  and the deterministic runtime executes all selected handlers.
- [`lib/web/pipeline-v1.mith`](lib/web/pipeline-v1.mith) and
  [`examples/typed-call-dag-web-synthesis.mith`](examples/typed-call-dag-web-synthesis.mith)
  extend the same closed composition boundary to router, handler, view and
  effect roles. One model forward answers all twelve finite questions; when
  every answer is admitted, the compiler seals them into three typed call DAGs
  before runtime dispatch.

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

## Typed synthesis without generated code

`mithril.synthesis` is a separate, model-using compiler front end. Ordinary
Mithril compilation and runtime dispatch stay deterministic and do not load a
model. The synthesis boundary is:

```text
SynthesizeWebApplication (.mith JSON-LD)
  -> validate imports, routes and ontology identity
  -> OaK OWL 2 RL materialization
  -> OaK SPARQL candidate discovery
  -> finite Choice questions for every route
  -> one trained OpenJev forward pass
  -> revalidate distribution, winner, confidence floor and model revision
  -> deterministic WebApplication compiler
  -> :mithril.web/v1 IR
  -> deterministic dispatch
```

The model cannot emit an identifier, route, body, effect or source fragment.
It can select only OWL/SPARQL-discovered imported component labels. The result
is refused unless it declares `generated_text: false`, covers the exact finite
candidate set, selects the distribution winner, meets the selected library
symbol's `confidenceFloor`, and carries an immutable 40-character model
revision. The resulting Web IR stores the selected symbol, basis-point
confidence and complete distribution as execution evidence.

The included synthesis example was exercised with the published trained
OpenJev artifact at revision
`19bf9a64815add579fbf6c907bef584d9277a8e4`. It selected the authored static
response handler at 7,676 basis points against a 6,000-point ontology floor;
dispatching `GET /hello` then returned the authored response with status 200.
The multi-route example additionally verifies that one forward pass can fill
several typed handler holes and that every resulting route is executable. With
the pinned model above, `/hello` selected the static handler at 6,870 basis
points, `HEAD /health` selected it at 6,919, and `/retired` selected not-found
at 6,296; all three exceeded the 6,000-point ontology floor. This is still
evidence for this closed two-candidate handler family, not a claim of arbitrary
code generation or general web-program synthesis.

`SynthesizeWebPipelineApplication` applies the same checks independently to
the router, handler, view and effect pools. Its output is a versioned call DAG
with fixed role order and dependencies: router → handler → view → effect. The
runtime checks that graph again and refuses malformed dependencies or unknown
operations. This remains constrained composition from an authored catalog;
the model still cannot create identifiers, strings, control flow or effects.

The currently pinned OpenJev revision is qualified for the earlier handler
family, but is not yet qualified for this wider four-role catalog. It can
complete the twelve decisions in one forward pass, while the compiler refuses
the artifact whenever any role falls below its ontology-authored confidence
floor. In the recorded run, the first exact-router decision scored 5,090 basis
points against its 5,500-point floor, so no artifact was emitted. The
deterministic tests exercise admitted call DAG construction and all runtime
branches; a successful test fixture is not reported as learned-model
qualification. New-role training and held-out calibration remain required.

## Bounded graph agent loop

[`lib/graph/agent-loop-v1.mith`](lib/graph/agent-loop-v1.mith) publishes the
closed collector, analyzer, governor and store catalog used by
[`examples/mithril-graph-agent.mith`](examples/mithril-graph-agent.mith).
`mithril.graph` compiles that JSON-LD source to `mithril.graph-agent/v1`: an
explicit source allowlist, byte/source budgets and four imported operations.
One tick is finite: collect declared public JSON-LD, summarize it, govern the
receipt, then store the immutable receipt as a Kotobase raw block. A durable
outer scheduler may repeat ticks; the graph IR itself contains no unbounded
internal loop and cannot invent a source URL or storage effect.

## Bounded application agent loop

[`lib/app/agent-loop-v1.mith`](lib/app/agent-loop-v1.mith) and
[`examples/mithril-app-agent.mith`](examples/mithril-app-agent.mith) define the
closed loop used by `app.mithril.fund`: OWL 2 RL inference, bounded SPARQL
component selection, SHACL Core validation, deterministic Web IR compilation,
conformance execution and a self-contained deployment URL. The corresponding
OWL vocabulary is published at
[`ontology/app-agent-v1.mith`](ontology/app-agent-v1.mith).

The user may author bounded display strings, but the agent does not generate
source text or identifiers. Template, data shape, operations and effects are
selected from the imported ontology catalog. The only admitted deployment
effect is a read-only, content-addressed URL carrying its complete source; it
does not grant server-side storage, DNS or Cloudflare authority.

`bin/mithril-run.cljk` executes a previously compiled Web IR artifact without
loading OpenJev. Inference is therefore a compile-time policy input rather than
ambient runtime authority:

```sh
kbb --backend sci --classpath src bin/mithril-run.cljk \
  compiled-artifact.json GET /hello
```

## Source contract

- `.mith` and `.mithril` are aliases. The suffix and surface syntax never enter
  semantic identity.
- A source beginning with `(` is read as one inert Mithril Form. No form is
  evaluated; unknown tags, fields, duplicate fields and trailing forms fail
  closed.
- Existing JSON-LD documents remain accepted. JSON-LD and Form sources that
  denote the same graph compile to the same canonical RDF Dataset digest.
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
kbb --backend sci --classpath "$CP" bin/mithril.cljk compile-ontology ontology/semantic-core-v1.mith
kbb --backend sci --classpath "$CP" bin/mithril.cljk compile-library lib/web/v1.mith
kbb --backend sci --classpath "$CP" bin/mithril.cljk compile-web examples/hello-web.mith lib/web/v1.mith
kbb --backend sci --classpath "$CP" bin/mithril.cljk request examples/hello-web.mith lib/web/v1.mith GET /hello
kbb --backend sci --classpath "$CP":test test/run.cljk
kbb --backend sci --classpath "$CP":test test/run_synthesis.cljk
amu check src/mithril/runtime.kotoba --jvm-free
```

The model-backed CLI additionally requires `OPEN_JEV_PYTHON`, `OPEN_JEV_SRC`,
`OPEN_JEV_MODEL` and an immutable `OPEN_JEV_REVISION`; there is no model ID
fallback in the compiler:

```sh
OPEN_JEV_PYTHON=/path/to/python \
OPEN_JEV_SRC=/path/to/typed-decisions \
OPEN_JEV_MODEL=your/pinned-open-jev-artifact \
OPEN_JEV_REVISION=0123456789abcdef0123456789abcdef01234567 \
kbb --backend sci --classpath "$CP" bin/mithril-synthesize.cljk \
  request examples/hello-web-synthesis.mith lib/web/v1.mith GET /hello
```

The CLI prints a JSON artifact containing the semantic graph digest and the
typed OaK transaction. The sibling `.mithril` example must compile to the same
semantic projection.
