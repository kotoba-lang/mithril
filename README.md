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

## Hermes profile fleet manifests

`mithril-hermes-fleet` projects every local Hermes profile and cron job into a
typed `.mith` manifest. It binds `config.yaml`, `SOUL.md`, the complete cron
registry, every prompt, schedule, resolved script and absolute coding workdir
by SHA-256 identity. Prompt prose is evidence only: executable authority is
derived into the closed `scheduler/tick`, `host/script-run`,
`llm/agent-turn`, or `agent/workspace-turn` effect catalog. A workdir is
context, not write authority: it becomes a coding-capable turn only when the
same absolute workspace has a valid digest-bound coding tool profile.

```sh
kbb --backend sci bin/mithril-hermes-fleet.cljk sync \
  "$HOME/.hermes/profiles" "$HOME/.hermes/mithril/profile-fleet-v1"
kbb --backend sci bin/mithril-hermes-fleet.cljk verify \
  "$HOME/.hermes/profiles" "$HOME/.hermes/mithril/profile-fleet-v1"
```

`verify` compares both the complete file set and every byte. A missing script
is retained as `:script-present false` and counted in the index; it is never
reported as an empty successful profile. These manifests establish the
governed migration and drift boundary. They do not by themselves intercept a
legacy Hermes cron execution; execution migration additionally requires the
Mithril scheduler adapter and a receipt for each effect.

`admit-coding` is the fail-closed migration gate. It measures every enabled
workspace turn against the live filesystem and coding registry and reports
`missing-workspace`, `not-git`, `dirty-workspace`,
`unregistered-workspace`, or `ready`. It exits non-zero until every candidate
is explicitly ready; a workdir alone never grants write or tool authority.

`plan-bpmn` reconciles the immutable `.mith` manifests with live scheduler
telemetry and produces a phased migration report. Jobs with active executions,
non-OK terminal state, a failure streak, missing scripts/workdirs, or no prior
observation are quarantined. Only healthy `--no-agent` script jobs enter the
first shadow cohort. Healthy LLM jobs remain `bpmn-contract-required`: their
prompt text is not translated into authority. They advance only after an
explicit ontology-bound BPMN action contract exists, and execution still
requires scheduler-originated receipts.

```sh
kbb --backend sci bin/mithril-hermes-fleet.cljk plan-bpmn \
  "$HOME/.hermes/profiles" "$HOME/.hermes/mithril/profile-fleet-v1"
```

`shadow-bpmn` replays one named job's latest Hermes execution into the
ontology-bound [`hermes-shadow-process-v1.mith`](resources/hermes-shadow-process-v1.mith).
It first verifies the entire profile manifest set, then requires a healthy
script-only job and a complete `source=builtin`, `status=completed` scheduler
receipt. The BPMN token must terminate before an immutable, idempotent local
receipt is written. This is observation-only: no Hermes job is disabled, no
script is rerun, and no tool effect is authorized by this process.

```sh
kbb --backend sci bin/mithril-hermes-fleet.cljk shadow-bpmn \
  "$HOME/.hermes/profiles" "$HOME/.hermes/mithril/profile-fleet-v1" \
  "$PWD/resources/hermes-shadow-process-v1.mith" \
  "$HOME/.hermes/mithril/bpmn-shadow-v1" cron-health b777d8b13259
```

The command deliberately takes an explicit profile and job ID; `plan-bpmn`
eligibility is not permission to turn on all jobs. LLM-driven jobs and actual
scheduler replacement remain separate migration phases.

### Executing BPMN scheduler adapter (read-only v1)

[`hermes-readonly-bot.mith`](examples/hermes-readonly-bot.mith) declares the
ontology-bound BPMN process, action set, effect grants, and retry/step budget.
[`hermes-readonly-scheduler.mith`](examples/hermes-readonly-scheduler.mith) is
the closed JSON-LD `.mith` binding to one Hermes profile, job, and compiled
bot digest. This is a canary contract, not a prompt-to-permission conversion.

Install [`hermes-bpmn-adapter.sh`](scripts/hermes-bpmn-adapter.sh) into that
profile's `scripts/mithril-bpmn-adapter.sh` and run the job as Hermes
`--no-agent --script mithril-bpmn-adapter.sh`, with the Mithril checkout as
`--workdir`. The adapter accepts exactly one fresh `source=builtin` running
claim from that profile's execution database, verifies the live job and its
compiled profile manifest, and performs at most one BPMN service task through
the existing Jev decision, governor, and deterministic effect executor. It
writes an occurrence-scoped attempt marker before any effect and a receipt
only after the BPMN state advances. A lost receipt is uncertain and refuses
automatic replay. Direct CLI calls with no scheduler claim are refused.

`verify-profile` checks just the bound profile against its manifest (other
profiles may change independently). V1 admits only `workspace/read`,
`git/status`, and `agent/stop`; write, compile, test, and other coding effects
need separate idempotency or reconciliation contracts before migration.

The coding executor now persists the `:awaiting` BPMN checkpoint and an
`effect-requested` audit record **before** calling the host. A restarted tick
with that checkpoint refuses `effect-in-flight`; it cannot silently select or
repeat another action. For a completed patch whose receipt was lost, an
operator can run `mithril-hermes reconcile` with the exact bot, task, state,
owner, and digest-bound coding tool profile. Reconciliation checks the
proposal, patch file, base HEAD, exact changed paths, disposable proposer
workspace bytes, and reverse-apply feasibility. It advances the BPMN token
from observed state without another target write:

```sh
kbb --backend sci bin/mithril-hermes.cljk reconcile \
  examples/governed-coding-bot.mith task.json state.edn operator coding-tools.json
```

Proposal artifacts can likewise be reconciled from their verified content;
compile, test, reset, and effects with ambiguous host state remain held. This
is the underlying write-recovery primitive, **not** authorization to add those
effects to the v1 scheduler contract or to auto-retry an uncertain occurrence.

### BPMN coding scheduler contract (v2)

The opt-in `HermesBpmnCodingSchedulerContract` adds an exact
`toolProfileDigest` binding to the v1 job, compiled bot, owner, task, and
effect grants. Its `task` includes the same `tool-profile-digest`; the adapter
checks both against the validated coding tool profile and requires its workspace
to equal the task workspace. V1 remains read-only. Install
[`hermes-bpmn-coding-adapter.sh`](scripts/hermes-bpmn-coding-adapter.sh) as
`mithril-bpmn-coding-adapter.sh` in the Hermes profile, and supply the closed
JSON-LD contract at `examples/hermes-coding-scheduler.mith` and coding tool
profile at `examples/hermes-coding-tools.json` in the job workdir. These files
must be generated for the actual job and workspace; they are not shipped as
usable examples because their profile, task, and tool digests grant authority
to exact local resources. The adapter does not generate authority from a prompt.

V2 keeps one task and a profile/job-keyed BPMN state file across cron
occurrences, including contract revisions, while writing
separate attempted/completed receipts for each scheduler execution. One
`source=builtin` running claim can advance exactly one BPMN service task.
Before execution it rejects a prior unreceipted attempt, an in-flight effect,
or a terminal state. A lost host receipt is held for explicit reconciliation,
not retried by a later cron tick. This is a bounded scheduler execution
contract, not a claim that arbitrary coding jobs are now reliable.

An opt-in `"checkpointMode":"ipld"` coding contract additionally records
each post-tick state as a v2 ontology-validated DAG-CBOR block. The previous
state must equal the verified CID head before the next effect; each successful
scheduler receipt includes `stateCid` and `semanticGraphDigest`. The local ref
update is serialized and compare-and-swap checked on one filesystem, not a
distributed consensus or crash-durable commit. Existing `legacy` and
`semantic` contracts do not change. `ipld` still uses the semantic EDN
execution path and verifies its CID witness before the next occurrence; the
RDF graph is not yet the runtime's sole state of record.

For a fresh coding run, the read-only reconciliation command compares every
Hermes `source=builtin` execution with its scheduler receipt, the exact BPMN
action, effect ID and output digest, semantic graph digest, and parent-linked
IPLD state:

```sh
kbb --backend sci bin/mithril-scheduler-ipld-verify.cljk \
  /absolute/state-dir <profile> <job-id> /absolute/profile/cron/executions.db
```

On 2026-09-23, an isolated real Hermes builtin profile exercised seven
occurrences: `workspace/read`, a fixed allowlisted proposer,
`workspace/apply-patch`, Mithril `compile-web`, `test/run`, `git/status`, and
`agent/stop`. An eighth builtin occurrence returned `terminal` with no
effect and the same final CID. The verifier found 7 completed effects, 7
linked semantic blocks, 1 terminal delivery, and 1 deliberately rejected
`source=direct` call. `GET /hello` returned the modified `.mith` body. This
does not prove Jev-authored code, Amu compilation, distributed refs, crash
durability, or parity across the wider Hermes fleet.

An isolated Hermes `source=builtin` canary on 2026-09-23 exercised a real
Mithril web-app edit over separate cron occurrences: `workspace/read`,
`llm/propose-patch`, `workspace/apply-patch`, `amu/compile`, `test/run`, and
`git/status`. The proposer was a fixed script in a disposable Git worktree;
`amu/compile` was bound in this canary to Mithril `compile-web`, not Amu itself.
The test dispatched `GET /hello` and checked the changed response. Every
occurrence had its own scheduler receipt and the same ontology-validated
semantic session lineage. Eight occurrences were all green while the bot was
still `running`: the final BPMN gateway had selected repeatable `git/status`
instead of `stop`. The coding selector now requires the tested/reviewed facts
and chooses `stop` at that gateway. A second builtin occurrence advanced a
verified fork of the step-8 immutable head to a `completed` run. A later
builtin occurrence on that terminal state returned a `terminal` receipt with
no effect ID and left the head unchanged. This establishes a bounded coding
loop and no-effect terminal redelivery in isolation; it does not establish
Jev-authored patches, actual Amu compilation, automatic distributed
branch/merge, or fleet-wide Hermes replacement. Receipt `status=completed`
means one effect completed; `runStatus` is the distinct workflow status.

### Private IPLD checkpoint branches (experimental)

`mithril-checkpoint-ipld` stores an ontology-validated bot state in canonical
DAG-CBOR blocks with real CID links to up to two parents. Each read rehashes the
block and rechecks the `.mith` projection, OWL entailment, SPARQL facts, and
SHACL shape. A private local adapter stores immutable blocks and named refs;
its one-filesystem lock and ref compare-and-swap prevent two local writers from
silently advancing the same name. For example:

```sh
kbb --backend sci bin/mithril-checkpoint-ipld.cljk import state.mith state.edn /absolute/private/store base
kbb --backend sci bin/mithril-checkpoint-ipld.cljk fork /absolute/private/store base left
kbb --backend sci bin/mithril-checkpoint-ipld.cljk assert /absolute/private/store left review/approved
kbb --backend sci bin/mithril-checkpoint-ipld.cljk verify /absolute/private/store left
kbb --backend sci bin/mithril-checkpoint-ipld.cljk show /absolute/private/store left
```

`import` requires the `.mith` file to be the exact projection of the paired
execution-state EDN. `merge` accepts only additive facts on two branches of
the same run; divergent effects, receipts, workflow tokens, or deletions are
rejected. This is an immutable, content-addressed branch experiment, not a
general Unison merge or a distributed store. It does not yet make the RDF
graph the execution state of record, sync refs between machines, provide
crash-durable fsync, or establish fleet-wide Hermes profile equivalence.

New checkpoint blocks use v2. Alongside the legacy `graphDigest`, v2 stores a
separate `semanticGraphDigest` computed from the
[`bot-checkpoint` context](resources/context-bot-checkpoint-v1.jsonld) with
absolute predicate IRIs and an explicit RDF field-loss check. The CID binds
the ontology source and `.mith` semantic projection. Existing v1 blocks remain
readable but do not acquire the stronger semantic digest retroactively. A
legacy `bot-run-v1` state may omit `task-digest`; the checkpoint derives it
from the preserved task value, while a mismatching declared digest or a
non-legacy omission is rejected.

On 2026-09-23, the actual `mithril-jev-readonly-canary` Hermes state was
imported unchanged into a private v2 store and independently verified as one
CID block with `runStatus=completed` and three effect receipts. Its latest
Hermes `source=builtin` execution was `completed`; the corresponding output
reported `idempotent-terminal` with the same run ID and status. This proves
that one existing scheduler-driven Mithril run can be rechecked as ontology-
projected content-addressed state. It does not prove equivalence for arbitrary
Hermes profiles, externally visible effects, or cross-host mutable refs.

### Replayable Jev decision evidence (experimental)

The legacy growth proposal receipt contains an RDF projection digest but not
the observation that Jev saw. The pinned v1 JSON-LD context also omits some
`GrowthProfile` policy terms: changing `metric` leaves its RDF graph digest
unchanged. The new `definition-digest` binds the complete checked profile;
existing v1 receipts retain their legacy digest and must not be read as proof
that every policy field was fixed.

For new isolated propose-only decisions, `decide-traced` records the complete
`.mith` profile, bounded observation, normalized Jev result, and typed receipt
in a private DAG-CBOR block. Its CID appears in the JSONL receipt. Later
decisions link to the preceding CID; duplicate observation IDs, untraced
legacy tails, receipt mutation, policy drift, and broken ancestry are refused.
The traced receipt also exposes `definitionDigest` for the complete checked
profile; its older `profile-digest` remains only the v1 RDF projection.
Trace v2 additionally projects the admitted decision through
[`growth-decision-v1.mith`](ontology/growth-decision-v1.mith) into a pinned
[`JSON-LD context`](resources/context-growth-decision-v1.jsonld). It verifies
the declared SHACL count/datatype constraints, an OWL 2 RL superclass
entailment, a SPARQL query, and an RDF Dataset canonical hash. The trace CID
binds the ontology source, semantic `.mith` source, and graph digest; missing
JSON-LD term mappings are rejected. This is the declared subset of OWL/SHACL,
not general OWL 2 or SHACL support. The verifier replays Mithril's decision
admission and semantic projection from the saved inputs:

```sh
kbb --backend sci bin/mithril-growth.cljk decide-traced \
  examples/itonami-labor-liberation.mith observation.json \
  /absolute/private/receipts.jsonl /absolute/private/blocks
kbb --backend sci bin/mithril-growth.cljk verify-trace \
  examples/itonami-labor-liberation.mith \
  /absolute/private/receipts.jsonl /absolute/private/blocks
```

A real OpenRouter Jev 1.13 call with `test/fixtures/growth-trace-smoke.json` on
2026-09-23 selected `hold-for-evidence` at 9,400 basis points, wrote one v2 CID
block, and passed a fresh-process `verify-trace` (1 receipt/1 semantic block).
The older v1 trace from the same day still verifies as one legacy block. This
proves deterministic replay of the recorded result; it does not prove that
the observation was true, that the provider signed its response, that an
external proposal effect occurred, or that the entire Hermes job is equivalent.
The local JSONL/block append is serialized among cooperating writers but has
no distributed CAS or crash-durable fsync guarantee.

An isolated 2026-09-23 migration audit of the current Hermes fleet found 286
profiles, 316 jobs, and 296 enabled jobs. Of the enabled jobs, 30 were
`shadow-ready`, 221 required explicit BPMN contracts, 26 were unhealthy, 16
had active executions, and 3 were unobserved. The coding admission gate found
only 1 ready job out of 127 coding candidates (82 dirty workspaces, 34
unregistered workspaces, 10 non-Git workspaces). The standing Mithril fleet
manifest had drifted, so this audit compiled a fresh copy in an isolated
temporary directory without replacing the standing manifest or enabling jobs.
These are admission counts, not behavioral parity or a replacement claim.

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
defines the executable bot policy and its ontology-bound BPMN process as
Mithril data. [`ontology/bpmn-agent-v1.mith`](ontology/bpmn-agent-v1.mith)
maps governed processes, decision gateways, service tasks and action bindings
to BPMN/OWL classes and constrains the bindings with SHACL. `mithril.bot`
performs the
OWL 2 RL / SPARQL candidate closure, validates the task with the declared SHACL
shape, and exposes only these typed actions: workspace inspection, patch
proposal, patch application, Amu compilation, test execution, git status and
stop.

The durable process authority is the pure token interpreter from
`org-omg-bpmn`. A Jev choice may select only a service task reachable from the
current BPMN exclusive gateway and whose ontology prerequisites hold. The
gateway moves to the task before effect dispatch; only a matching successful
receipt advances that task to the next gateway. A failed receipt restores the
prior gateway, so retry never guesses or reconstructs control flow. Hermes
Kanban is now only the delivery and handoff projection for this profile, not
the source of process order.

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
  -> BPMN token enables reachable service tasks
  -> OpenRouter TypeSafe Jev typed action choice
  -> Governor capability intersection
  -> one deterministic host effect
  -> content-addressed receipt
  -> next BPMN token state
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

An opt-in `semantic-execute` mode uses the same one-effect loop but commits
each requested and completed state to an immutable, parent-linked block under
`state.edn.semantic-blocks/`. The head is `state.edn.semantic-head`; the EDN
file remains a local projection. The block contains a `.mith` BotCheckpoint
form, RDF-canonical graph digest, full state digest and ontology digest. Its
RDF projection includes facts, BPMN token position, pending effect, and receipt
count; OWL materialization, SPARQL fact query and the declared SHACL shape are
executed before commit. The complete runtime state is still an EDN value
inside the private block, so RDF is not yet the sole replay representation.
[`bot-checkpoint-v1.mith`](ontology/bot-checkpoint-v1.mith) declares its OWL
class and SHACL shape; the writer executes both checks, and the reader verifies
the full ancestry before continuing. A legacy EDN-only state is refused by
this mode rather than silently imported. Existing state can be imported
explicitly with `mithril-hermes semantic-import <profile.mith> <task.json>
<state.edn> <owner>` after profile/task binding verification. Terminal
redelivery remains a no-op, in-flight effects still require reconciliation,
and a new task can follow a terminal run in the same causal chain. `reconcile`
recognizes a semantic head and advances that same chain after an in-flight effect. These private
blocks are local, not published to a public IPLD store. This proves a bounded
content-addressed checkpoint/lineage path, not distributed Unison branch/merge
or parity with every Hermes profile and cron job.
If a crash occurs after the head commit but before its EDN projection is
rewritten, `semantic-execute` refuses `state-diverged`. An operator can run
`mithril-hermes semantic-restore <state.edn>`: it verifies the ontology and
entire block ancestry, rewrites only the EDN projection, and records an audit
event. It does not replay an effect.

An explicit Hermes BPMN scheduler contract may set `"checkpointMode":
"semantic"` (or `:checkpoint-mode "semantic"` in its `.mith` form). The
scheduler then calls `semantic-execute`; a pre-existing EDN-only coding session
requires the explicit import above. Jev admission refusals such as confidence
below the ontology floor become an effect-free `held` checkpoint and a
non-success `held` scheduler receipt. Later occurrences return `held` without
calling the model or host effect again. This is a safe terminal handoff, not a
claim that the bot completed its task.

On 2026-09-23, an isolated Hermes profile was exercised through the actual
`cron tick` scheduler, with a one-occurrence read-only contract using
`checkpointMode=semantic`. Execution `e53fd1d823fe406393990720b4efc2a2`
was recorded by Hermes as `source=builtin`, `status=completed`; the matching
Mithril receipt bound the same execution and job IDs to one `workspace/read`
effect and its output digest. `semantic-verify` checked the parent-linked
head (`sha256:87d90b6aa2e8f467cb09a1b331db853bb4a92a76738856dabb5db1236852b735`),
including its OWL entailment, SHACL shape, and SPARQL fact projection. The
OpenRouter Jev request used 418 input and 36 output tokens (reported cost
0.000017556 USD); the two audit rows refer to the same request, not two model
calls. A separate `cron run` occurrence had `source=direct` and was correctly
refused with `not-scheduler-originated`. This is scheduler-originated evidence
for one read-only service task, not coding completion, fleet parity, or a
token-free loop. The isolated profile was repeat-limited and is terminal.

`shadow` records the decision but requests no effect and does not advance the
state. Without a coding tool profile, `execute` admits only `workspace/read`,
`git/status`, and `agent/stop`. Workspace and Git
observations are reduced to counts/booleans before hashing, so command output,
file names, and the workspace path are absent from stdout and the audit log.
The state lock fails closed on overlap. If its recorded PID is provably absent,
the next tick atomically renames that stale inode, acquires a replacement lock,
and appends a `stale-lock-recovered` audit event. An unreadable lock or a live
PID is never removed automatically, so ambiguity remains an operator recovery
instead of silently overriding a lease.

The checkpoint is content-bound to both the compiled bot profile and the exact
task. A running checkpoint refuses a different profile or task. Re-delivery of
the same completed or held task is an idempotent terminal result and executes
no model or host effect; a task with a distinct content identity starts a new
bounded run. This makes ordinary cron/webhook at-least-once delivery safe
without a canary-specific completed-state wrapper.

The BPMN profile explicitly lists the content identities of legacy profiles it
can migrate. A quiescent v1 checkpoint with the exact task, admitted legacy
policy, known monotonic facts, and no in-flight effect is upgraded by replaying
its completed semantic actions through the BPMN interpreter. The migration
preserves step, retry, and effect receipts and appends a content-digested
semantic migration receipt. Unknown profiles, facts, inconsistent fact chains,
or in-flight effects fail closed instead of guessing a token position.
Migration is an explicit, effect-free operation and is idempotent:

```sh
kbb --backend sci bin/mithril-hermes.cljk migrate \
  examples/governed-coding-bot.mith task.json state.edn worker-1
```

It atomically replaces only the checkpoint and appends a
`checkpoint-migrated` audit event; it does not call Jev or a host tool.

For a coding run, the optional final argument is a JSON tool profile. The task
must contain the digest printed by `profile-digest`:

```sh
kbb --backend sci bin/mithril-hermes.cljk profile-digest coding-tools.json

kbb --backend sci bin/mithril-hermes.cljk tick \
  examples/governed-coding-bot.mith task.json state.edn worker-1 \
  workspace/read,llm/propose-patch,workspace/apply-patch,amu/compile,test/run,git/status,agent/stop \
  execute coding-tools.json
```

The coding profile fixes an absolute isolated Git workspace, disjoint exact
read-only, editable, and creatable file lists, byte/time budgets, one Hermes proposer profile, and exact argv for
compile and test. The host creates a detached disposable Git worktree at the
bound base `HEAD`, then invokes the Hermes proposer with only the `file`
toolset. The proposer may inspect declared read-only context and patch only the
editable/creatable paths in that scratch worktree, never the target workspace.
The host ignores prose, rejects read-only, untracked/non-allowlisted changes,
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

Compile or test failure enters a bounded repair edge instead of blindly
re-running the same command. The host stores a size-limited diagnostic in the
private artifact directory, records only its digest and byte count in the
append-only audit, and exposes it only to the next patch advisor. A separate
`patch-reset` effect reverses the exact digest-bound proposal in both target
and disposable worktrees before the next proposal. `maxRetries` bounds this
cycle; an exhausted run is held rather than reported as completed.

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

## Governed growth loops

`mithril/growth-profile` is the propose-only scheduler surface for bounded
business interventions. A profile declares one metric and a finite catalog of
typed actions. OpenRouter TypeSafe Jev selects one action directly; Mithril
rejects generated text, incomplete probability distributions, candidates
outside the catalog, low-confidence choices, and every authority other than
`propose-only`. The CLI appends a content-bound JSONL receipt and never sends
outreach, publishes content, or mutates a live service:

```sh
kbb --backend sci bin/mithril-growth.cljk decide \
  examples/murakumo-advertiser-acquisition.mith \
  observation.json receipts.jsonl
```

The shipped profiles cover labor liberation, advertiser acquisition, and user
acquisition. Their action descriptions are decision criteria, not generated
copy; external execution remains a separately governed effect.

## Run

```sh
CP=src:../org-w3-json-ld-api/src:../org-w3-rdf-canon/src:../org-w3-nquads/src:../io-multiformats/src:../text/src:../org-nist-sha2/src
kbb --backend sci --classpath "$CP" bin/mithril.cljk compile examples/tender.mith
kbb --backend sci --classpath "$CP" bin/mithril.cljk compile-ontology ontology/semantic-core-v1.mith
kbb --backend sci --classpath "$CP" bin/mithril.cljk compile-library lib/web/v1.mith
kbb --backend sci --classpath "$CP" bin/mithril.cljk compile-web examples/hello-web.mith lib/web/v1.mith
kbb --backend sci bin/mithril.cljk emit-desktop examples/mithril-desktop.mith
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
