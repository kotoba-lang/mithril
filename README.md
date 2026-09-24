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
The profile's `graph-digest`/contract `profileDigest` now binds the executable
declaration, not Hermes's mutable `enabled` state or its derived `jobsDigest`.
`verify-profile` still parses and checks the stored Mith declaration and
refuses changed script, schedule, prompt, workdir, or effects. The scheduler
separately requires a live builtin claim and checks the current job before
execution. Existing contracts must be regenerated against a freshly synced
fleet manifest when upgrading this identity rule.
The read-only contract may opt into `checkpointMode=ipld-primary` only when
the Hermes job has `repeat.times=1`. Its single builtin occurrence can take
a Jev decision, commit the v2 ontology-validated decision output before the
host read, and link the host receipt to the CID head. A second occurrence is
refused rather than silently reusing a per-execution state path.

An isolated one-occurrence Hermes job on 2026-09-24 exercised this path with
`source=builtin`, `status=completed`. OpenRouter Jev selected
`workspace-inspect`; the host completed one `workspace/read`. The primary
verifier reconciled the Hermes execution and receipt with three causal CID
blocks (baseline, Jev v2 decision/effect request, host result), zero direct
executions, and a matched EDN projection. Its final state CID was
`bafyreif4pr2ny3zfdlyel7flmdimuhthaa74zaxdklig4farvxaawvvmqa`.
The single Jev request reported 781 input and 36 output tokens, cost
$0.000032802; the two audit rows share the same request ID. The repeat-limited
job disabled itself after completion. This proves one scheduler-originated
read-only loop, not Jev-authored coding, fleet parity, token-free inference,
or distributed mutable refs.

A separate isolated v7 canary on 2026-09-24 exposed two adapter faults:
read-only Jev was passed as the first optional CLI argument and interpreted as
a tool-profile filename (`open 'jev'`), while runtime job state could drift
from a stored profile snapshot. The corrected canary `e49cb6abfe41` completed
one `source=builtin` execution (`f6904aa038374a9ebb63e5332d998227`).
OpenRouter Jev selected `workspace-inspect` at confidence 9900; the governor
admitted one `workspace/read` effect. The scheduler/IPLD verifier reported
one completed effect, zero direct executions, three causal blocks, and a
matched projection at CID
`bafyreicailevwd6pngwjl7uh6jkwryd7vvnedtk64yjuhoz25pjgz4jx4q`.
Independent v7 checkpoint verification found the Jev-output RDF Dataset with
13 triples. Its provider usage was 776 input and 36 output tokens, cost
$0.000032592. This is one read-only scheduler step, not a coding-workflow or
fleet-equivalence qualification; the failed one-shot diagnostics were retained
and never replayed. Focused fleet/scheduler and Jev tests passed; the full
test runner was interrupted during `checkpoint-ipld-test` after several
minutes without a result, so it is not reported as green.

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
Coding keeps its deterministic selector by default. A coding contract may
explicitly set `"decisionMode":"jev"` only with
`"checkpointMode":"ipld-primary"`: when BPMN exposes multiple admitted
actions, OpenRouter Jev chooses one typed action ID from the ontology-checked
state. The Governor still checks capabilities, and the v2 decision output is
committed to the CID chain before the host effect. A singleton action stays
deterministic. This option does not let Jev generate a code patch; the separate
proposer and tool profile remain responsible for that effect.
In an isolated 2026-09-24 builtin coding occurrence, Jev selected
`workspace-inspect` from three BPMN-admitted actions. The scheduler executed
`workspace/read`; independent primary verification matched one Hermes
execution, the Jev v2 decision, host receipt, and three CID blocks. A second
multi-occurrence canary reached `held` when Jev's next choice fell below the
ontology confidence floor. The held transition is effect-free and now returns
its CID; independent verification matched two builtin executions, one host
effect, one held delivery, and four causal blocks. The failed pre-fix
occurrence remains in its isolated evidence directory and was not replayed.
Neither canary proves Jev-selected patch proposal/write, an entire coding
workflow, or fleet-wide Hermes parity.

The optional `bin/mithril-openrouter-proposer.cljk` is a bounded generative
advisor for the coding profile. Set `MITHRIL_PATCH_MODEL` to a configured
OpenRouter chat model and supply `OPENROUTER_API_KEY` (or the existing exact
Keychain item). Set the tool profile's proposer `program` to this executable
and `profile` to `typed-mithril-v1`. It receives only the explicitly listed
regular files, returns one inert `(mithril/edit-proposal :path ... :old ...
:new ...)` form, and performs one exact, unique-span replacement in the
disposable proposal worktree. The host independently validates the resulting
patch, allowed paths and checked compile/test effects. The model receives no
shell or file tools. The allowlist comes from the digest-bound tool profile
as a separate typed argument, never from prompt markup. The edit shape is
declared in `ontology/edit-proposal-v1.mith` with OWL and SHACL; the supported
SHACL count/datatype subset is executed at admission. The proposal artifact
stores the Mithril form, its canonical document digest and the patch digest;
reconciliation verifies both. This document digest is not an RDF canonical
dataset identity.

An isolated 2026-09-24 **direct** Jev/IPLD loop used a real OpenRouter model
proposal, then patch application, Mithril `compile-web`, request assertion,
Git status and stop. Seven effects completed; the primary IPLD verifier
accepted 15 causal blocks at the final CID. `GET /hello` returned the changed
body while HEAD remained unchanged. This proves that bounded code proposal
can use Mithril input/output in this one local case; it does **not** establish
scheduler-originated real-model execution, arbitrary coding, Amu compiler
parity, fleet migration, or full Hermes tool/delivery equivalence.

A separate isolated v7 Hermes `source=builtin` job on 2026-09-24 completed
the full linear BPMN coding path: Jev selected `workspace-inspect`, then the
checked one-way transitions proposed and applied an allowlisted `.mith` patch,
ran Mithril `compile-web`, asserted `GET /hello` returned HTTP 200 with the
changed body, recorded Git status and stopped. Seven scheduler executions
completed with zero failed effects and zero direct executions. Independent
primary verification matched all receipts, 15 causal blocks and the EDN
projection at final CID
`bafyreica3hxne32nk44cadv4ueke4yuiuwvkplq45pabwvxikiadpbl4we`.
The one Jev request reported 789 input and 36 output tokens, cost
$0.000033138. This canary used a fixed reviewed proposer, not a model-authored
patch, and the `amu/compile` effect was bound to Mithril `compile-web`, not
the Amu compiler. An initial isolated job refused a missing artifact directory
before any effect; the successful job used a new ID rather than replaying the
failed attempt. Both jobs were paused. This establishes one scheduler-driven
closed loop, not fleet-wide Hermes parity or a speed advantage: its seven
occurrences took 51.9 to 147.5 seconds each.

Another isolated v7 Hermes job (`eda7d7f99bdb`) on 2026-09-24 joined the
previously separate scheduler and real-model paths. Jev selected
`workspace-inspect`; the auxiliary OpenRouter model returned one typed
`(mithril/edit-proposal ...)` for the allowlisted `examples/hello-web.mith`.
The host admitted and applied only the GET `/hello` body edit, preserving the
HEAD route. Mithril `compile-web`, an actual HTTP 200 response assertion, Git
status and stop then completed. All seven `source=builtin` executions finished,
and the repeat-limited job disabled itself. Independent primary verification
reported seven completed effects, no failed or direct effects, 15 causal CID
blocks and a matched projection at
`bafyreig6y3vrenugkkppy6kkkzx36lipx75nbfst5wnvwkgufd3z7iklty`;
independent v7 checkpoint verification agreed on the head and completed state.
The Jev call used 793 input and 36 output tokens ($0.000033306); the patch
model call used 803 prompt and 209 completion tokens ($0.00022495). Those are
provider-reported subtotals, not an all-in cost comparison with Hermes.
Occurrence durations ranged from 31.3 to 107.4 seconds in this one run.
The `amu/compile` effect still invoked Mithril `compile-web`; real Amu
compilation, arbitrary coding, fleet-wide parity, and distributed refs remain
unverified.

The primary scheduler verifier now reuses its fully checked CID nodes within
one verification call. It still rehashes every reachable block and reruns
Mithril/OWL/SPARQL/SHACL validation on each new invocation; the reuse only
removes redundant semantic decoding when matching the same blocks to Hermes
receipts. On one 15-block isolated builtin history, the same verifier command
returned identical results in 76.89 seconds before and 37.16 seconds after
this change (2026-09-24, one local measurement each, approximately 2.07x).
Tests assert one block read per invocation and rejection when an ancestor
returns bytes for another CID on a later invocation. This is not a persistent
trust cache or an IPQ completeness proof. A cross-tick cache would require an
explicit versioned trust anchor, ref-head binding and tamper/upgrade tests.

`mithril.checkpoint-ipq` provides a separate, bounded transport proof for an
immutable checkpoint head. `export-history!` verifies the complete Mithril
history, selects its parent CID links, and writes a root-first CARv1;
`verify-history-car!` replays the selector against only the CAR bytes and then
reruns Mithril's ontology, SHACL, graph-digest and causal-transition checks.
`import-history!` writes the checked immutable blocks to a separate local
store, re-verifies them from that store, and only then publishes a new ref.
The imported ref can be forked and its additive fact branches merged locally;
an existing ref is never overwritten by import.
The caller supplies the expected root; the archive cannot choose it. The
profile is limited to 32 blocks, 64 selector path components, 4 MiB of selected
blocks and 4 MiB plus framing allowance for the input CAR. It rejects missing,
altered, excess or semantically invalid blocks. This is not yet an HTTP IPQ
endpoint, a completeness proof for arbitrary queries, a cross-tick trust
cache, or synchronization of mutable refs.

```sh
kbb --backend sci bin/mithril-checkpoint-ipq.cljk export \
  /absolute/checkpoint-store session-ref /absolute/history.car
kbb --backend sci bin/mithril-checkpoint-ipq.cljk verify \
  /absolute/history.car bafy...expected-head
kbb --backend sci bin/mithril-checkpoint-ipq.cljk import \
  /absolute/other-checkpoint-store received /absolute/history.car bafy...expected-head
```

On 2026-09-24 the isolated seven-occurrence model-authored scheduler canary
exported a 15-block, 1.1 MiB CAR. Offline verification returned the same
head `bafyreig6y3vrenugkkppy6kkkzx36lipx75nbfst5wnvwkgufd3z7iklty` and
semantic graph digest `sha256:07ff91cd7cfbd749f1418352367cd411a96ccb7d2790cb5229da1f5e62034355`
in 23.04 s on one local run. The test suite pins refusal reasons for a
foreign root, missing ancestor, altered CID bytes, unused block and changed
ontology digest. Transport does not make semantic verification free. Import
into a distinct store is a local immutable-history transfer, not distributed
mutable-ref synchronization or a remote IPQ service.

The Node-only IPLD store now installs Node SHA-256 through the existing
`multiformats/install-sha256!` agreement gate before CID reads and writes.
It does not bypass rehashing or semantic validation. On the same isolated
15-block verifier fixture, three sequential cold A/B pairs returned the same
CID and result: portable wall times 38.07/32.53/23.97 s and host-digest times
34.85/17.51/20.48 s (2026-09-24). These are load-sensitive local samples,
not a fleet speedup or a cache/IPQ measurement. IPQ would help bounded remote
block selection; a cross-invocation semantic cache still needs a checked
trust anchor and schema/ontology version binding.
Before execution it rejects a prior unreceipted attempt, an in-flight effect,
or a terminal state. A lost host receipt is held for explicit reconciliation,
not retried by a later cron tick. This is a bounded scheduler execution
contract, not a claim that arbitrary coding jobs are now reliable.

An opt-in `"checkpointMode":"ipld"` coding contract additionally records
each post-tick state as a v4 ontology-validated DAG-CBOR block. The previous
state must equal the verified CID head before the next effect; each successful
scheduler receipt includes `stateCid` and `semanticGraphDigest`. The local ref
update is serialized and compare-and-swap checked on one filesystem, not a
distributed consensus or crash-durable commit. Existing `legacy` and
`semantic` contracts do not change. `ipld` still uses the semantic EDN
execution path and verifies its CID witness before the next occurrence; the
RDF graph is not yet the runtime's sole state of record.

The read-only scheduler verifier reconstructs the final state from the CID
named by completed builtin receipts, even if the EDN projection is missing;
an existing projection must match exactly. If the projection is missing at a
later coding cron occurrence, the scheduler checks the complete prior Hermes
execution/receipt series, the CID ancestry, exact task/profile binding, and
the independently verified semantic head before recreating the EDN projection.
It then treats a completed run as a no-effect terminal delivery. A missing or
divergent semantic head, unreceipted effect, or ambiguous execution history is
not auto-repaired. This is recovery of a derived projection, not a claim that
all execution state now lives solely in RDF/IPLD.

An additional opt-in coding contract, `"checkpointMode":"ipld-primary"`,
uses the local ontology-validated CID ref as the *executable* state authority.
The EDN file is a checked, recoverable projection: a missing file is rebuilt
from the verified CID ancestry, while a divergent file is refused. Each effect
commits a requested/in-flight CID **before** the host call and a receipt-applied
CID afterwards. A retry encountering an in-flight head refuses to repeat the
effect; `mithril-hermes reconcile` can consume durable proposal/apply evidence
without re-execution, and missing evidence remains held for inspection. Each
state path is one run; a different task requires a new path rather than a
silent ref reset. The verification command accepts a trailing `primary` to
check both phases, Hermes occurrences, BPMN actions and terminal deliveries.
`ipld-primary` does not auto-import the older semantic/EDN session.

In a separate isolated Hermes profile on 2026-09-23, seven distinct builtin
occurrences advanced read, fixed patch proposal, patch application, Mithril
`compile-web`, test, git status and stop. The primary verifier found 14 linked
semantic blocks (request/result for every effect), seven effect receipts, and
one terminal delivery. The edited `.mith` app answered `GET /hello` with HTTP
200 and the expected changed body. With the EDN projection withheld, a second
builtin terminal delivery reconstructed it from the CID head without adding
an effect or block. An earlier isolated attempt reached a durable in-flight
proposal when its artifact directory was missing; it was not re-run. Coding
profile preflight now checks that directory before effect admission, while
reconciliation explicitly refuses `proposal-missing`. A separate CLI test
confirmed that an already persisted proposal artifact can advance an in-flight
CID head without invoking the proposer or changing the workspace. This
experiment used a
fixed proposer and Mithril `compile-web`, not a Jev-authored patch or Amu
compilation. Local refs still lack distributed CAS, crash-durable fsync, and
fleet-wide behavioral parity.

The v3 writer was then exercised by an isolated Hermes builtin cron job
`266224087204` on 2026-09-23. Seven scheduler occurrences produced seven
effect receipts and 14 request/result CID blocks; every block was independently
read as v3 with a typed `stateNode` and no `stateEdn`. The verifier reported
`completed`, seven effects and 14 blocks at final CID
`bafyreidoq6z45mmkhivx2h2wsvqll3eyd4awzxubldvlwapl4e7slcvrc4`.
`GET /hello` returned HTTP 200 and the changed body. Two subsequent builtin
occurrences returned terminal receipts with no new effect or block. Before the
second, the EDN projection was withheld; the run reconstructed it from the
verified CID head and the verifier reported a matching projection. A separate
intentionally misbound job refused execution and then refused continuation
without its prior receipt. This remains a fixed-proposer, single-host canary,
not evidence of Jev-authored coding or fleet-wide parity.

On 2026-09-24, isolated Hermes builtin job `929467bfa38b` exercised the
v4 RDF-Dataset writer through all seven coding effects. The primary verifier
accepted 14 linked request/result blocks and two later no-effect terminal
deliveries at final CID
`bafyreifwpidqiq6nor7distcg6ybh6r67tizo2uvo3gy2o6puuwxezcely`.
All 14 blocks contained a compact `stateDataset`, with neither `stateEdn` nor
`stateNode`; the `.mith` app returned HTTP 200 with the edited body. The
second terminal delivery reconstructed a deliberately withheld EDN projection
from the CID head. In one local final-state measurement, v4 used 92,031 bytes
versus 37,009 for v3 on the same state (2.49x); an earlier single v4 block
write/read measured 0.7/1.1 seconds. These are individual measurements, not
fleet latency or cost qualification. The proposer was fixed, not Jev-authored;
`amu/compile` invoked Mithril `compile-web`, not Amu.

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

A copied isolated profile was also exercised after its EDN projection was
withheld. CID-only verification still matched all seven effects. One new
`source=builtin` cron occurrence restored the projection from the checked CID
state and returned `terminal` with the same final CID and no effect ID; the
complete copied series then verified with seven effects and two terminal
deliveries. The original canary evidence was left untouched.

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
kbb --backend sci bin/mithril-checkpoint-ipld.cljk domain /absolute/private/store left
kbb --backend sci bin/mithril-checkpoint-ipld.cljk decision-output /absolute/private/store left
kbb --backend sci bin/mithril-checkpoint-ipld.cljk upgrade /absolute/private/store left left-v7
```

`import` requires the `.mith` file to be the exact projection of the paired
execution-state EDN. `merge` accepts only additive facts on two branches of
the same run; divergent effects, receipts, workflow tokens, or deletions are
rejected. This is an immutable, content-addressed branch experiment, not a
general Unison merge or a distributed store. It does not sync refs between
machines, provide crash-durable fsync, or establish fleet-wide Hermes profile
equivalence. `domain` emits the verified v6/v7 domain RDF view as JSON-LD;
`decision-output` exposes the v7 Jev-output RDF Dataset, and `verify` reports
separate domain and decision-output graph digests. `upgrade` re-encodes a
verified causal history under a new ref, leaving the source ref unchanged.
It refuses publication if the source head advances during conversion.

New checkpoint blocks use v7. Their complete executable state is a canonical
RDF Dataset stored as a compact IPLD `stateDataset` (term dictionary plus
indexed triples). The reader expands its quads, reconstructs the state from
them, and rejects noncanonical or orphaned graph nodes; it does not read an
EDN string or v3 typed tree to recover v6/v7. The same graph can be projected as
JSON-LD. The reader continues to verify v1 through v6 blocks under their original
identities rather than rewriting history. Alongside the legacy `graphDigest`,
v2 through v7 store a separate
`semanticGraphDigest` computed from the
[`bot-checkpoint` context](resources/context-bot-checkpoint-v1.jsonld) with
absolute predicate IRIs and an explicit RDF field-loss check. The CID binds
the ontology source and `.mith` semantic projection. Existing v1 blocks remain
readable but do not acquire the stronger semantic digest retroactively.
The original domain-level OWL/SHACL graph remains a checked, lossy summary;
v5 through v7's structural RDF Dataset carries the complete runtime state, and a
CID-bound [`state-graph-v1.mith`](ontology/state-graph-v1.mith) ontology now
checks its node types by OWL 2 RL subclass entailment and its entry, item,
value and scalar fields by a bounded SHACL Core subset (count, datatype,
class). V6/v7 also store a separate, compact `domainDataset` projected
deterministically from that executable state. The CID binds both datasets,
the versioned [`bot-domain-v1.mith`](ontology/bot-domain-v1.mith) OWL/SHACL
ontology, and `domainGraphDigest`, which hashes the canonical RDF Dataset
independently of the block CID. `Run`, `Receipt`, `AwaitingEffect`, `Action`,
and `Fact` become queryable nodes. Receipt action identity is reconstructed
from the run ID, step index, and closed action catalog; ambiguity is refused.
OWL 2 RL derives `Run` and effect-event superclasses; the declared SHACL
shapes check count, datatype and class; a real SPARQL query checks receipt and
fact links. Incoming v6/v7 blocks are rederived from the full state rather than
trusting a supplied projection. Unknown shapes and constraints are refused.
This is not full OWL 2 or full SHACL Core, and it does not make every nested
field independently queryable as a domain predicate. A legacy `bot-run-v1`
state may omit `task-digest`; the checkpoint derives it
from the preserved task value, while a mismatching declared digest or a
non-legacy omission is rejected.

V7 also stores `decisionOutputDataset` and its independent RDF graph digest.
It is assembled from each admitted Jev decision's v2 `.mith` output form:
the reader re-parses that form, checks its declared shape and proof-bound
fields, and rejects a digest or source mismatch. The dataset contains typed
decision nodes without copying the model's full result text into this
queryable projection. The exact checkpoint CID still binds the complete
state, all three RDF datasets, and their ontology sources. This is not a
general schema-evolution mechanism: historical ontology files and the
checkpoint schema must remain available for old CIDs.

On 2026-09-24, the isolated real-Jev Hermes canary's 17-block v6 history was
re-encoded under a new v7 ref without changing the v6 ref. Both heads
verified with the same final executable state and domain graph digest; the
v7 output dataset contained one Jev decision and 13 RDF triples. This is
local migration evidence, not a live-fleet cutover or distributed Unison sync.

A separate direct `ipld-execute` v7 run exposed a continuation bug: after the
first live Jev-selected `workspace/read`, resuming renewed the lease but used
the previous CID as the parent of the next Jev proof. Parent replay correctly
refused `invalid-decision-record` without advancing the effect. The adapter now
commits that renewed decision state before asking Jev, so the proof refers to
its exact CID parent. The same isolated run then completed two `git/status`
effects. When its three-step budget was exhausted, it committed an effect-free
`held` state; re-delivery returned `idempotent-terminal` with the same CID.
Independent whole-history verification accepted all 12 v7 blocks and the
decision-output RDF digest; its packed Dataset held 39 triples for three
Jev decisions. This is direct CLI evidence, not Hermes
`source=builtin` scheduler evidence or a completed coding workflow.

The three successful model decisions recorded 2,441 input tokens, 112 output
tokens and $0.000102522 in OpenRouter usage. Four diagnostic retries before
the fix reached Jev but failed during proof validation and had no audit row,
so total experimental cost is **unmeasured**, not the recorded subtotal. New
calls append a `model-observed` audit row with request ID and usage immediately
after the provider response, before proof construction or an effect. A fresh
live v7 run verified that row precedes `effect-requested` and `completed` for
the same request ID (844 input tokens, 44 output tokens, $0.000035448).
This still does not establish a latency or cost advantage over Hermes.

The verified seven-effect v5 Hermes final state was read under its historical
CID and emitted as an in-memory v6 block: exact state equality, 93 domain
quads, seven SPARQL receipt rows, eight fact rows, and a 109,082-byte block
(one local measurement). That first check covered only the v6 value layer;
the separate scheduler run is recorded below. `domainGraphDigest` is semantic
RDF identity; the block's CID is causal storage identity. Neither by itself
supplies distributed mutable refs, general branch merge, or crash-durable
persistence.

An isolated v6 Hermes `source=builtin` coding job `ba648cbf4be7` then ran
on 2026-09-24 with the v6 writer in the actual scheduler path. Its nine
occurrences all completed: seven governed effects (read, fixed patch
proposal, apply, Mithril `compile-web`, response test, Git status, stop)
followed by two no-effect terminal deliveries. The primary verifier matched
all nine Hermes execution rows to scheduler receipts and 14 linked v6
request/result blocks, with no direct executions, and reported
`projection=matched`. Before the ninth delivery, the EDN projection was
withheld; the scheduler reconstructed a semantically equal value from the
verified CID head without another effect or block. The final CID was
`bafyreib5h3e3eqpp2wsgvtbyzcjk443f3y3z4q7so6f7nad3bbtolgbe3i`.
Independent checkpoint verification returned v6 format and
`domainGraphDigest=sha256:1694eecd60656226fdcf7b1c1b81df7a0f6d42c2e0f531fff88a3ce33d394061`;
the emitted domain graph had 23 typed nodes, including seven receipt and
eight fact nodes. The edited `.mith` app returned the expected HTTP 200
body. The repeat-limited job ended disabled; the live fleet was not changed.
The seven effect occurrence durations in this one run were 16.5, 39.8,
54.9, 76.6, 96.2, 120.4 and 138.5 seconds, respectively, so v6 currently
does not establish a speedup; growing-chain verification needs profiling and
optimization. This remains a fixed proposer and Mithril `compile-web` canary,
not Jev-authored code, real Amu compilation, distributed ref semantics or
fleet-wide profile parity.

A read-only profile of that same 14-block store measured 2.68 seconds for
one head read and 46.55 seconds for whole-history verification. The verifier
was then changed to fully read and ontology-validate each unique CID once,
including shared ancestors, instead of rereading every parent before visiting
it. The same whole-history command took 25.12 seconds and returned the
identical verification result (one local before/after measurement, about
1.85× faster). A negative test still rejects corruption of a shared ancestor.
This is an offline verification improvement, not a post-change Hermes tick
or fleet latency qualification; the complete history is still checked on
every scheduler occurrence.

The final state from the isolated seven-effect Hermes builtin run was also
read from its historical v4 CID, emitted as an in-memory v5 block, and read
back with exact state equality. That v5 block was 97,941 bytes (one local
measurement); no scheduler occurrence was switched to the v5 writer for this
check, so it does not establish live v5 scheduling or fleet parity.

On 2026-09-24, a separate repeat-limited isolated Hermes builtin job
`c3b941b4fec6` exercised the v5 writer in the actual scheduler path. Seven
occurrences completed the governed read, fixed-proposer patch, apply, Mithril
`compile-web`, HTTP-response test, Git status, and stop effects; two later
occurrences delivered the same terminal state without another effect. The
primary verifier reconciled all nine `source=builtin` executions with their
receipts and 14 linked CID blocks (`status=verified`, `runStatus=completed`,
`projection=matched`). All 14 blocks are v5 RDF datasets with the same
CID-bound `.mith` state-ontology digest. Before the second terminal delivery,
the EDN projection was withheld; the reader rebuilt it from the CID head.
`GET /hello` returned HTTP 200 and the edited `.mith` response body. The job
ended disabled after its ninth repeat. This proves one scheduler-driven v5
closed loop, not fleet-wide behavioral parity or Jev-authored coding; the
proposer was fixed and `amu/compile` still invoked Mithril `compile-web`.

The setup also exposed a recovery boundary: `hermes cron run` created a
`source=direct` execution and was correctly refused. A separate initial
builtin job with a missing artifact directory failed before writing a receipt;
the next occurrence refused `missing-prior-receipt` rather than silently
repeating an uncertain action. That job was paused and a fresh, prepared
isolated job was used for the completed run. Preflight admission and explicit
reconciliation of pre-effect failures remain needed before unattended fleet
migration.

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

On 2026-09-24, a fresh isolated manifest sync of the live local Hermes fleet
found 286 profiles, 316 jobs, 296 enabled jobs, and no missing scripts. The
read-only BPMN plan classified the enabled jobs as 30 `shadow-ready`, 211
`bpmn-contract-required`, 27 `active-execution`, 25 `unhealthy`, and 3
`unobserved`. Coding admission of 127 candidate jobs found 1 `ready`, 87
`dirty-workspace`, 29 `unregistered-workspace`, and 10 `not-git`. An existing
isolated seven-effect Hermes builtin coding run was independently reverified
against its scheduler database as 7 completed effects, 14 semantic blocks,
and 2 terminal deliveries; this is one representative closed workflow, not
fleet-wide equivalence. No live job was switched off or migrated by the audit.

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
  -> domain RDF materialization + OWL/SHACL validation
  -> compact Mithril decision-state S-expression
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
The Jev `state` field now carries a compact `(mithril/decision-state ...)`
form derived from the validated domain RDF graph: ontology and graph digests,
task identity, current facts, causal receipt-action IDs, and the exact
BPMN-admitted candidate action IDs. The workspace path is omitted. Candidate
criteria are `(mithril/action ...)` forms; Jev's returned choice is still
mapped to a catalog action and checked by the Governor. The audit records
the decision-form, domain-graph, and ontology digests. A real read-only
OpenRouter Jev call on 2026-09-24 accepted this form and selected
`workspace-inspect` (confidence 0.64); it reported 847 input and 44 output
tokens, cost $0.000035574. This command emitted an effect request but did not
execute a host tool. The API still tokenizes the S-expression input and bills
output tokens for its typed choice; this is not token-free inference. That
standalone `mithril-jev next` observation did not commit a CID checkpoint.

In the opt-in IPLD-primary loop, a Jev-selected effect now appends a typed
`decide/jev` receipt to the executable state *before* invoking the host. It
binds the validated Mithril input and RDF/ontology digests, normalized model
result, selected action, exact effect ID, and provider request ID. The state
RDF Dataset and DAG-CBOR CID therefore commit to this decision output. On
ref/history verification, the parent state is used to reconstruct the
candidate set, re-admit the recorded choice, and replay the effect request;
altered or missing decision evidence is refused. This is local causal replay,
not a provider-signed response or proof that the model's choice was correct.
The v6 domain RDF projection still describes facts and effects rather than
individual decision receipts. V7 adds a separate persisted decision-output
RDF Dataset; older checkpoint formats remain readable.
New Jev decisions additionally carry a v2 `mithril/jev-decision` output form.
[`jev-decision-v2.mith`](ontology/jev-decision-v2.mith) declares its own
`JevDecision` class, `SemanticDecision` superclass, and exact-field SHACL
shape; the output is lowered to JSON-LD with a pinned context, expanded to
RDF, and given a canonical graph digest. The writer executes the declared
SHACL subset and an OWL 2 RL entailment/SPARQL query before committing the
proof. Parent replay regenerates the output and rejects changes even if an
attacker recomputes the proof's outer digest. Historical v1 decision proofs
continue to replay without being recast as v2. This is a semantic projection
of the decision result, not proof of a provider-signed Jev response, full
OWL 2/SHACL Core support, or an RDF-only executable state.
An isolated live OpenRouter `ipld-execute` check on 2026-09-24 selected
`workspace-inspect` with confidence 0.99 and completed a real read-only
`workspace/read` effect. A fresh process verified its three-block causal
chain (pre-decision state, requested effect with Jev receipt, applied host
receipt), ending at CID
`bafyreigzs5r3umsjr2lvc2wdcqlobxgybcnszgr74qv4mcnej33nfxlhoi`.
OpenRouter reported 844 input and 44 output tokens, cost $0.000035448.
This direct isolated tick was not `source=builtin` from Hermes cron; it does
not establish scheduler-originated or fleet-wide behavioral parity.

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

`examples/governed-coding-linear-bot.mith` is an opt-in, straight-through BPMN
profile for a reviewed coding task. Its first gateway offers Jev the genuine
choice to inspect or stop. Once inspection is admitted, one-way, checked BPMN
gateways require proposal, patch application, compilation, test, Git review,
and stop in that order. The executor still checks each action's prerequisites,
capability grant, host receipt, and primary IPLD checkpoint; a one-way gateway
is not ambient permission. This profile intentionally has no repair branch:
failed effects stay subject to the bounded retry/hold policy. Use the general
`governed-coding-bot.mith` profile for governed reset/reproposal. The reviewed
proposer remains a separate tool; Jev selects an action, not a patch body.

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
