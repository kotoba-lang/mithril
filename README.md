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

## Hermes coding workspace

`mithril.desktop` compiles a `DesktopApplication` into a one-file Hermes
Desktop plugin from a pinned template. The Mithril document admits exactly
nine host actions: profile and session discovery, create/resume, prompt,
status/history, interrupt and stop. It cannot name an arbitrary RPC method or
inject JavaScript.

The generated application provides four projections over one semantic state:
Coding, Chat, Sessions and Bots. A coding turn binds an absolute local
workspace and a live Hermes profile to a typed `mithril/coding-request`.
Sessions can be resumed, chat continues the same runtime session, and each
transition is appended to plugin-scoped receipt storage. The user commits a
bounded task; the model proposes work; Hermes executes tool calls and checks;
the governor records `committed`, `held` or `refused`. One click runs one
bounded turn—there is no immortal loop inside the plugin.

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

## Governed coding bots

[`examples/governed-coding-bot.mith`](examples/governed-coding-bot.mith)
defines the executable bot policy as Mithril data. `mithril.bot` performs the
OWL 2 RL / SPARQL candidate closure, validates the task with the declared SHACL
shape, and exposes only these typed actions: workspace inspection, patch
proposal, patch application, Amu compilation, test execution, git status and
stop.

OpenRouter's real `typesafe/jev-1.13` is restricted to one finite `Choice`
distribution over the actions whose prerequisites are true. It cannot emit
source, identifiers, tool names or arguments. The independent Governor must
grant the selected capability before the runtime emits one effect. Exactly one
matching receipt advances the state; effect IDs are content-derived, duplicate
receipts fail closed, and lease, retry and total-step budgets are carried in
the state. Jev's `confidence` is intentionally checked separately from the
winning option probability; read-only actions and mutating actions have
different ontology-authored floors.

```text
.mith BotProfile
  -> OWL 2 RL action entailment
  -> bounded SPARQL candidate query
  -> SHACL task validation
  -> OpenRouter TypeSafe Jev typed action choice
  -> Governor capability intersection
  -> one deterministic host effect
  -> content-addressed receipt
  -> next semantic state
```

Compile the published profile with:

```sh
kbb --backend sci --classpath "$CP" bin/mithril.cljk \
  compile-bot examples/governed-coding-bot.mith
```

Call Jev and emit one governed effect request with:

```sh
kbb --backend sci --classpath "$CP" bin/mithril-jev.cljk \
  next examples/governed-coding-bot.mith task.json worker-1 \
  workspace/read,git/status,agent/stop
```

`task.json` contains `id`, `workspace`, and `goal`; a coding task additionally
binds the SHA-256 `tool-profile-digest`. The credential is
read at call time from `OPENROUTER_API_KEY`, then from the exact macOS Keychain
item `service=gftd.openrouter account=OPENROUTER_API_KEY`; it is never written
to the artifact or receipt. The adapter pins the request to
`typesafe/jev-1.13`, records OpenRouter's resolved model identity, request id,
usage and cost, and refuses malformed responses before the Governor runs.

### Hermes cron canary

`mithril-hermes` is the durable outer-loop adapter for Hermes `--no-agent`
jobs. One invocation acquires the state lease, asks TypeSafe Jev for at most
one finite decision, intersects it with Governor grants, executes at most one
host effect, atomically checkpoints the Mithril state, and appends a JSONL
audit record. A final singleton `stop` action is deterministic and does not
spend a model request.

```sh
kbb --backend sci bin/mithril-hermes.cljk tick \
  examples/hermes-readonly-canary.mith task.json state.edn worker-1 \
  workspace/read,git/status,agent/stop shadow

kbb --backend sci bin/mithril-hermes.cljk tick \
  examples/hermes-readonly-canary.mith task.json state.edn worker-1 \
  workspace/read,git/status,agent/stop execute
```

`shadow` records the decision but requests no effect and does not advance the
state. Without a coding tool profile, `execute` admits only `workspace/read`,
`git/status`, and `agent/stop`. Workspace and Git
observations are reduced to counts/booleans before hashing, so command output,
file names, and the workspace path are absent from stdout and the audit log.
The state lock fails closed on overlap. A crashed process may leave that lock
file behind; removing a stale lock is an operator recovery action rather than
an automatic lease override.

For a coding run, the optional final argument is a JSON tool profile. The task
must contain the digest printed by `profile-digest`:

```sh
kbb --backend sci bin/mithril-hermes.cljk profile-digest coding-tools.json

kbb --backend sci bin/mithril-hermes.cljk tick \
  examples/governed-coding-bot.mith task.json state.edn worker-1 \
  workspace/read,llm/propose-patch,workspace/apply-patch,amu/compile,test/run,git/status,agent/stop \
  execute coding-tools.json
```

The coding profile fixes an absolute isolated Git workspace, an exact file
allowlist, byte/time budgets, one Hermes proposer profile, and exact argv for
compile and test. The host creates a detached disposable Git worktree at the
bound base `HEAD`, then invokes the Hermes proposer with only the `file`
toolset. The proposer may read and patch that scratch worktree, never the target
workspace. The host ignores prose, rejects untracked/non-allowlisted changes,
and extracts the proposal from `git diff`. It then checks profile and patch
digests plus path/mode/binary constraints before running `git apply --check`
and writing the target. Compile and test use `execFile` with the profile's argv and no shell;
neither Jev nor the proposing model can invent a command or argument. Patch
text and tool output stay in the private artifact directory and are represented
in state/audit only by digests and bounded counts.

The linear coding prerequisites have exactly one useful next transition, so a
coding-profile run advances them in ontology order rather than asking Jev to
guess a mechanical workflow step. Jev remains the policy for genuine finite
choices; it is not used as mutation authority when the action catalog has not
been calibrated. The audit distinguishes `deterministic-ontology-workflow`
from an OpenRouter Jev decision.

The Hermes Desktop artifact embeds the same closed action/effect catalog. Cron
creation refuses duplicate enabled routines and the configured active-job
ceiling. Kanban task identity is SHA-256 derived from profile, workspace, title,
body and the Desktop graph digest, so retries reuse the task rather than
creating parallel work. Kanban execution also receives the compiled lease and
retry budgets.

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

## Hermes Desktop application

`mithril.desktop` compiles a `mithril/desktop-application` form to a checked
Desktop IR and then emits the single-file Hermes Desktop plugin. The source can
select only the closed `session/create -> prompt/submit -> session/status ->
session/history -> stop` state machine; RPC names, JavaScript and ambient host
capabilities are compiler-owned. Poll count and interval are bounded, and the
generated UI uses the Hermes plugin SDK and its theme variables.

The compiler output is ordinary deterministic build output. It is not source
text proposed by the agent. The agent receives a Mithril-form request and the
host executes each admitted transition mechanically.

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
