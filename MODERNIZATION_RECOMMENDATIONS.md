# GeneWarrior Modernization Recommendations

Last updated: 2026-10-16, including T-Coffee guide-tree cards and SVG/Newick export,
alongside Docker deployment and Primer3 internal probes. This supersedes the original pre-rewrite assessment. Phase
descriptions
below remain the target design; implemented scope and remaining gates are identified separately.

## Executive Summary

GeneWarrior has a substantial modern core, but the migration is not feature-complete.
A Svelte/TypeScript workspace and typed Spring Boot APIs now coexist in the source
tree with the opt-in legacy Thymeleaf/jQuery application. Core sequence workflows and pairwise alignment are
implemented. Complete workspace persistence and FASTA/CLUSTAL/print exports are now
implemented, as are native Java-calculated/SVG-rendered sequence logos. External-tool
execution now supports T-Coffee and Primer3 through bounded, database-backed jobs. Complete legacy
code retirement remains unfinished. Docker runs the modern frontend/API and PostgreSQL;
T-Coffee and Primer3 are bundled and tested offline. Primer designs have typed settings,
source snapshots, result cards and persistence/share/export support.
T-Coffee guide trees now have a typed asynchronous workflow and native SVG rendering;
they are explicitly not presented as an independent phylogenetic inference.

The next priorities are the remaining advanced tools and
public-release hardening, including representative tool workload benchmarks.
Browser regression coverage and targeted interaction cleanup accompany those changes.

## Architecture Decisions

- **Keep Spring Boot as the backend.** It provides the JSON API, biological operations,
  shared-project persistence and external-tool execution. No backend framework rewrite
  is planned.
- Keep the backend at the repository root with Java 25 and Gradle. The reviewed build
  uses Spring Boot 3.5.14 and Gradle 9.3.0.
- Use Svelte 5, TypeScript and Vite in `frontend/`. This is a Vite app, not a SvelteKit
  application. The earlier React recommendation has been superseded.
- Keep frontend development independent: Vite serves the UI and proxies `/api` to
  Spring Boot.
- For a single deployment, build the frontend and package its assets with Spring Boot.
  Spring can serve the UI as static files while continuing to handle the API. The
  build integration is implemented through Gradle's `frontendDist` property and the
  multi-stage Dockerfile; the production profile serves the compiled workspace at `/`.
- Keep H2 for local development/tests and PostgreSQL for persistent deployment.
- Keep the existing GeneTools algorithms behind focused services initially. Replacing
  them with BioJava is not a prerequisite for the migration.
- Use synchronous operations for fast tools and persistent asynchronous jobs for
  expensive external tools. Prefer dedicated services over a generic plugin framework.
- No authentication is planned. Existing legacy share links and payloads need not be
  backward-compatible.
- Docker and Compose are now in scope, superseding the earlier deferral. Target one
  GeneWarrior application image containing the compiled frontend, Spring Boot and
  required CLI tools, plus a separate PostgreSQL service.
- Replace MUSCLE with T-Coffee for multiple sequence alignment. Keep the existing
  Java pairwise algorithms. Replace the legacy MUSCLE tree dependency separately.
- Sequence logos now use Java calculation plus Svelte/SVG rendering. WebLogo, Python,
  Ghostscript and generated-image storage are not required by the modern logo workflow.

Replacing the legacy frontend does not mean replacing Spring. Thymeleaf, jQuery and
the form-based dispatch endpoint can be retired while Spring continues to serve both
the modern API and the compiled frontend in the application image.

## Current Implementation

### Backend

The modern path uses Java records, Jackson, Bean Validation and focused services:

| Area                          | Status                                                                                                                                     |
|-------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| Sequence import               | Implemented through `POST /api/sequences/import`                                                                                           |
| Reverse/complement transforms | Implemented through `POST /api/transforms`                                                                                                 |
| Translation                   | Implemented through `/api/translations`, including best ORF, frame preview and selected-range translation                                  |
| Pairwise alignment            | Implemented through `POST /api/alignments/pairwise`; cost-free ends and local alignment use existing algorithms                            |
| Consensus sequence            | Implemented synchronously through `POST /api/alignments/consensus`; validated DNA/protein voting returns an ordinary sequence              |
| Sequence logos                | Implemented synchronously through `POST /api/logos`; native counts, frequencies and uniform-background information content                 |
| Project storage               | Complete workspace snapshots saved/loaded through `/api/projects`, with structural validation and HTTP 413 size errors                     |
| Jobs                          | Persisted input snapshots, atomic single-worker claims, leases, cancellation, expiry, idempotent submission and scoped browser polling     |
| External tools                | T-Coffee and Primer3 run through the bounded runner and shared durable queue; Primer3 supports standard pairs and optional internal probes |
| Errors and validation         | Structured API errors and basic validation exist; limits and validation are not yet consistent                                             |

Focused services include `SequenceImportService`, `SequenceTransformService`,
`TranslationService`, `AlignmentService`, `ProjectSharingService`,
`SequenceLogoService`, `TcoffeeService`, `PrimerDesignService`, `GuideTreeService`, `JobService` and
`ExternalToolRunner`.

The legacy source is retained for comparison, with explicit isolation:

- `MainController` and `DnaToolsService` require `legacy & !production`; production
  never registers their routes, even if both profiles are selected.
- `DnaToolsService` remains approximately 2,176 lines and still handles legacy
  operations, HTML construction, Gson serialization and direct process execution.
- Thymeleaf, jQuery/jQuery UI sources, `outputTypes`, the old project entity, Gson and
  Lombok remain. Legacy static assets/templates are omitted from the release JAR.
  Spring Data REST was removed to avoid unintended repository HTTP endpoints.
- Legacy and domain code still contain `System.out` / `System.err` logging.

### Frontend

The new workspace implements:

- FASTA/plain-sequence import and DNA/protein cards.
- Project and sequence renaming, card selection, reordering, deletion and collapse.
- Subsequence dragging, position/range readouts, reverse-direction DNA selections and
  clipboard copying.
- Background highlights, annotation arrows, annotation chips and subsequence extraction.
- Reverse, complement and reverse-complement actions.
- Translation with genetic-code selection, six frames, best ORF and an interactive
  forward-frame modal with start/stop markers and selection preview.
- NCBI BLAST form submission for selected DNA or protein sequences.
- DNA/protein search with ambiguity symbols, both DNA strands, alignment search and
  blue hit highlighting.
- Pairwise DNA/protein alignment creation.
- Alignment rendering with consensus coloring, numbered row legend, hover feedback,
  column selection/copying and annotation overlays across wrapped blocks.
- FASTA sequence downloads, CLUSTAL alignment downloads and print/PDF output.
  Exports use selected cards by default, or all cards when none are selected; an
  explicit All entries scope overrides selection. Entry order is preserved.
- DNA/protein sequence-logo cards, frequency/information scale switching, position and
  coverage tooltips, SVG download, wrapped printing and complete workspace persistence.

These features exist in code; comprehensive browser regression verification is still
missing. Prior interactive fixes repeatedly exposed hidden Svelte reactive dependencies.

Tool control status:

| Control                 | Status                                                                                                                                                |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| Multiple alignment      | T-Coffee enabled for supported same-kind inputs when its startup probe succeeds; queued/running/cancel/retry UI and refresh recovery                  |
| Consensus sequence      | Implemented for every selected alignment; creates a standalone DNA/protein card immediately below its source, with persistence/sharing/export support |
| Guide tree (T-Coffee)   | Implemented for selected alignments; asynchronous Newick guide-tree generation, SVG rendering, downloads and persistence                              |
| Sequence logo           | Implemented for selected alignments, using synchronous Java calculation and native SVG rendering                                                      |
| Primer design           | PCR dialog with target/product/oligo/reaction constraints, optional internal probe, result map, oligo export and complete persistence                 |
| Export FASTA            | Implemented: full standalone DNA/protein sequences, 70-character lines, sanitized filenames/headers                                                   |
| Export CLUSTAL          | Implemented: full alignments, 60-column blocks, gaps/consensus, unique row identifiers; multiple alignments bundled as separate files in a ZIP        |
| Print                   | Implemented: dedicated expanded-card layout, annotations/colors, pagination, native browser printing; transient UI state unchanged                    |
| Share project           | Implemented: complete snapshots, closable link dialog with Copy link, frontend loading and separate local shared drafts                               |
| Delete selected entries | Confirmation dialog with selected-entry count, Cancel as initial focus and animated removal after confirmation                                        |

Most orchestration and tool UI remain in the approximately 1,200-line
`frontend/src/routes/App.svelte`. Sequence and alignment viewers are separate
components, but tool/domain modularization is incomplete.

### Persistence

Flyway migration `V1__modern_api_schema.sql` creates the legacy project table plus
`shared_project` and `tool_job`.

Implemented foundations:

- Unique random share keys.
- Project JSON text storage.
- `Instant` timestamps and fields for last access and expiry.
- PostgreSQL configuration with credentials supplied through environment variables.
- Hibernate schema validation in the main configuration and tests.

Completed in step 1:

- `ProjectDto` and the project store now include sequences, alignments, all marks,
  project/entry names and explicit entry ordering.
- Project edits, derived entries, deletion and navigator moves update the same store.
- Browser autosave restores the workspace before rendering. Empty projects remain empty.
- Modern links use `?project=<shareKey>` on the frontend URL. Each share creates a new
  immutable server snapshot; opening it restores the complete project.
- Shared drafts are autosaved under separate browser keys, preserving the local
  workspace and the original server snapshot.
- Invalid local data is left untouched; storage/quota failures are reported. Failed
  share loads offer retry or return to the local workspace.
- Server and browser validate entry IDs/order, alignment dimensions and annotation
  bounds/colors. Oversized server saves return HTTP 413.
- Local H2 uses `data/genewarrior` on disk with schema validation. Tests still use
  in-memory H2. The data directory is ignored by Git.

Remaining gaps: expiry assignment/enforcement and cleanup; PostgreSQL integration
verification; packaging/routing the new frontend into the Spring deployment.
Selection, hover state and collapsed cards remain transient UI state.

### Native Sequence Logos

`POST /api/logos` now returns HTTP 200 with a `SequenceLogoDto`, not an external job.
`SequenceLogoService` computes counts and heights; `frontend/src/lib/logo/` handles
pure layout, SVG rendering and download. No production dependency was added.

- Equal-length DNA/protein rows, equal sequence weights; default view is frequency.
- DNA alphabet A/C/G/T, case-insensitive, with U normalized to T. Protein alphabet
  contains the standard 20 amino acids. Gaps are counted separately; ambiguity symbols,
  protein stops and noncanonical residues are excluded, not redistributed.
- For each column, frequencies use the count of canonical residues as denominator.
  An all-gap/unknown column has zero frequency and zero information, with its original
  position retained. Hover tooltips report counts, gaps, ignored symbols and coverage.
- Information uses a uniform background: `R = log2(K) + sum(p * log2(p))` and each
  letter's height is `p * R`, for K=4 or 20. No pseudocounts, small-sample correction,
  sequence weighting or confidence intervals. The information view is labelled
  uncorrected; results should not be described as WebLogo-equivalent estimates.
- Limits: 2,000 columns, 10,000 rows and 2,000,000 input alignment cells. Oversized
  calculations return HTTP 413. Persisted statistics are checked against counts on
  both server and browser; nonfinite, inconsistent or unsupported data is rejected.
- One immutable data snapshot per selected alignment is inserted below its source.
  Scale changes, ordering, deletion and collapse use normal workspace behavior.
  Deleting/editing the source does not invalidate an existing logo.
- Project JSON has an additive optional `logos` collection; existing drafts and shared
  snapshots without it load as an empty collection. Each logo has a versioned
  `uniform-uncorrected-v1` method identifier. No database migration is needed for the
  existing JSON-text project storage. Logos retain data after refresh and sharing.
- SVG export embeds glyph outlines and their license, independent of installed fonts.
  The print layout splits logos into page-breakable SVG rows. FASTA/CLUSTAL exclude
  logo cards; native PNG export is not included (SVG and browser print/PDF are available).
- Glyphs derive from DejaVu Sans Bold; notices ship in
  `frontend/public/licenses/dejavu.txt`. Regenerate with `tools/GenerateLogoGlyphs.java`.
  Source font SHA-256: `e1d733afbbfce842a1b43dced0c9885a76a4e67290dc7e690c4e9b76ebfc7d23`.

Tests include analytic conserved/uniform/3:1 distributions, protein and ambiguity
handling, bounds, invalid persisted values, wrapped geometry, source deletion and
legacy-project loading. Browser coverage includes multi-alignment creation, immediate
scale updates, desktop/mobile layout, share loading, print/PDF and standalone SVG
parsing/rasterization with a nonblank colored-pixel assertion.
An independent development-only comparison against Logomaker 0.8.7 (uniform DNA
background, zero pseudocounts) matched the Java conserved, uniform and 3:1 information
heights within 1e-12. Logomaker is not a project/runtime dependency.

## Important Technical Gaps

### External Tool Execution

Initial T-Coffee alignment/guide-tree and Primer3 workflows are implemented (details
below). Model-based phylogenetic inference remains separate and is not packaged. Tool availability is exposed
separately from application readiness through `GET /api/tools/tcoffee` and
`GET /api/tools/primer3`. Primer3 startup checks exercise thermodynamic data as well
as the version probe, not just executable-file availability.

Remaining hardening includes adversarial process-tree tests (especially instant
double-fork/detached descendants), representative maximum-size workload measurements,
per-tool resource budgets when more tools are enabled, and controlled operational
diagnostics. Current process tracking terminates observed descendants; Docker PID/RAM/
tmpfs limits and tini remain essential containment, not an optional optimization.

### Validation And Security

Security headers and environment-based database credentials have been added. The
Svelte UI uses escaped rendering rather than the legacy string-built HTML approach.

Work still needed:

- Centralize sequence, project and computational limits, including pairwise alignment
  work limits.
- Validate sequence alphabets, mark bounds, direction/type values and project structure
  consistently across endpoints.
- Extend HTTP 413 handling beyond project saves to other oversized API payloads.
- Treat restored projects as untrusted input.
- Keep the legacy download endpoint isolated until deletion or filename sanitization.
- Production CSP no longer permits inline scripts; inline styles remain required for
  sequence rendering. NCBI BLAST form submission is explicitly allowlisted.
- Retire unsafe legacy HTML construction and direct external-tool calls with the
  corresponding legacy workflows.

### Structure And Tooling

- Split `App.svelte` into focused workspace, card and tool components.
- The unified project model/store is implemented; continue extracting tool UI from
  `App.svelte`.
- Make reactive dependencies explicit and isolate reusable mark/selection/layout logic.
- Keep cheap deterministic browser operations in pure TypeScript functions. Search,
  marking and extraction already run locally; transforms and translation still call
  the backend. Web Workers are deferred until large-input behavior warrants them.
- Frontend asset packaging and production routing are implemented; retain regression
  coverage for share links, static assets and real API/asset 404 responses.
- Finish revising historical README sections; modern development and export instructions
  are now documented separately from the legacy setup.
- Add CI for backend tests, frontend checks/tests/build and browser smoke tests.
- Minimal Actuator liveness/readiness is implemented, including PostgreSQL readiness.
  Formatting/linting configuration, automated dependency scanning and operation
  metrics remain to be added.
- `npm audit` on 2026-10-15 reports eight existing dependency advisories (three
  moderate, five high). Dependency upgrades remain separate from the export work;
  no advisory was reported for the newly added `fflate` ZIP library.

## Verification And Test Coverage

Verified on 2026-10-16 (source-export and restart-recovery checks were last run on 2026-10-15):

| Check                                          | Result                                                                                                                                                                   |
|------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GRADLE_USER_HOME=.gradle-home ./gradlew test` | Passed; 72 backend tests, including guide-tree parsing/topology/validation, Primer3, job lifecycle, persistence, runner limits and production isolation                  |
| `npm --prefix frontend run check`              | Passed; zero errors/warnings                                                                                                                                             |
| `npm --prefix frontend test`                   | Passed; 45 tests, including guide-tree layout/validation/Newick, search, persistence, export, logos, primers and resumable jobs                                          |
| `npm --prefix frontend run build`              | Passed                                                                                                                                                                   |
| `npm --prefix frontend run test:e2e`           | Passed; 21 browser workflows against Docker/PostgreSQL on port 8081, including real guide-tree SVG/Newick/share/print, mobile labels and the minimum-row constraint      |
| `docker compose build` / `up -d --wait`        | Passed; independent multi-stage build, PostgreSQL migration/schema validation, healthy app and database                                                                  |
| Container recreation/persistence               | Passed; the same saved project loads after both app and database containers are recreated with the existing volume                                                       |
| PostgreSQL job recovery                        | Passed; a queued fixture persisted while the app was stopped completed after startup; an expired RUNNING lease became FAILED/INTERRUPTED, with both lease slots released |
| Offline final-image scientific fixtures        | Passed as non-root with no network, read-only root, noexec scratch and resource limits                                                                                   |
| Tool source export                             | Passed; matching distro source archives/patches and SHA-256 inventory exported separately                                                                                |

Backend coverage includes import/detection/invalid characters, reverse-complement mark
conversion, translation tables/frames/ORFs/range translation, consensus extraction,
native logos, context startup, eleven modern API tests and three production-profile
tests. H2 context tests exercise migration boot/schema validation; the Docker smoke
test also exercises the real PostgreSQL V2 migration, atomic job claims, real DNA/protein
alignment execution and snapshot round trip. Fake-runner tests cover queue/rate limits,
idempotency, cancellation/completion races, lost/expired leases and result expiry.
Primer3 tests cover typed requests, right-primer coordinates/orientation, internal
probe placement, malformed output and no-pair results. Docker smoke checks real
pair-only and probe designs, target flanking and PostgreSQL project round trips.
Browser tests verify probe options, result placement, refresh, sharing, FASTA/print,
mobile dialog controls and a placeholder visible before the submission response.
Guide-tree tests cover multifurcations, duplicate names, signed/scientific/zero lengths,
malformed or missing leaves, topology validation, bounded input and project round trips.
Real Docker smoke checks DNA and protein guide trees and rejects two-row requests
before execution. Browser checks include desktop/mobile SVG rendering, safe long
labels, topology fallback, refresh, SVG/Newick downloads, sharing and printing.

Coverage is still missing or insufficient for:

- Pairwise alignment modes and biological edge cases.
- Broader mark editing, selection and annotation layout cases.
- Adversarial detached process behavior and high-contention multi-instance job claims.
- Payload/coordinate limits and comprehensive API error contracts.
- Broader PostgreSQL crash/recovery and expiry load tests.
- Broader end-to-end coverage beyond persistence/share-link and export Playwright tests.

Persistence tests cover independent snapshots, annotations, ordering, deletion,
empty workspaces, corrupt local data, storage failures and invalid shared payloads.
Modal browser tests cover delete confirmation/cancellation, Escape/backdrop dismissal,
keyboard focus containment/restoration, share copying and clipboard/API failures,
and desktop/mobile layouts.
Browser tests exercise pairwise creation, annotation, reordering, refresh, sharing
to a fresh context, shared draft recovery and missing-link recovery. Desktop/mobile
screenshots were inspected. Local share survival across backend restart was checked.
These checks do not establish full UI correctness or external-tool readiness.

Export tests cover selected/all scope, project ordering, full sequence content,
FASTA header injection prevention, safe filenames, duplicate CLUSTAL identifiers,
ZIP downloads, empty workspaces and annotation continuation. Browser printing is
tested without changing selection, collapse state or autosaved content. Generated
A4 PDFs were inspected, including a complete 3,600-base sequence spanning four pages.
Print preserves annotations and backgrounds; FASTA/CLUSTAL are sequence-data formats
and do not carry workspace annotations. Static HTML export remains legacy-only.

## Recommended Next Work

1. **Complete workspace persistence: done.** Unified model/store, server round trips,
   share-link loading, browser autosave/recovery and initial browser regression tests.
2. **Finish exports: done.** FASTA, CLUSTAL/ZIP and Print are wired with selected/all
   scope, sanitized filenames and unit/browser tests. Export logic and print layout
   live in `frontend/src/lib/export/` rather than in the workspace component.
3. **Prove the containerized toolchain: initial baseline done.** Offline T-Coffee and
   thermodynamic Primer3 and guide-tree fixtures pass. Full reproducibility and release
   licensing/security review remain. See `docker/README.md`.
4. **Runner/job pipeline: initial implementation done.** Persisted input, single-worker
   leases, bounded execution, cancellation, expiry and browser polling are wired and tested.
5. **Replace MUSCLE with T-Coffee: initial workflow done.** Real DNA/protein execution,
   verified results, nullable score, provenance and share/export integration work.
   Broader alphabet support and representative capacity benchmarks remain deliberate follow-ups.
6. **Migrate remaining tools and their persistence.** Initial Primer3 pairs/internal
   probes, consensus extraction, native logos and T-Coffee guide trees are implemented.
   Model-based phylogenetic inference is not included. Primer overhangs,
   forced primers and genomic specificity checks remain outside the initial scope.
7. **Finish the Docker release gates.** Svelte/Spring packaging, Compose/PostgreSQL,
   secrets, health and production legacy isolation are implemented. Public-service
   resource/abuse controls, dependency remediation, backups and CI still need work.

Add focused browser regression coverage and extract tool orchestration from
`App.svelte` throughout this work. Authentication, Kubernetes, remote T-Coffee modes
and a generic plugin framework remain outside this implementation wave.

## Docker And External Tools Plan

### Implemented First Increment

`Dockerfile`, `.dockerignore`, `compose.yaml`, `.env.example` and `docker/README.md`
now provide a working localhost deployment. `docker/init-secrets.sh` provisions the
ignored secret file without overwriting existing credentials. Classpath Spring
configuration replaces root property files; H2 remains local/test-only in this setup.
The app uses UID 10001, a read-only root, bounded scratch/CPU/RAM/PIDs, `tini`, graceful
shutdown and DB-aware readiness. No Docker socket or host scientific tools are needed.

Image digests and direct scientific package versions are pinned. Ubuntu's T-Coffee
package `13.45.0.4846264+really13.41.0.28bdc39+dfsg-1` actually reports upstream
13.41.0.28bdc39; Primer3 is 2.6.1-4 with `/etc/primer3_config/`. Explicit internal
`slow_pair,lalign_id_pair` methods pass DNA/protein residue-preservation fixtures
without MUSCLE or networking. Tiny alignments used about 475 MiB peak RSS; one tool
job at a time remains the intended initial policy. Thermodynamic primer design also
passes. T-Coffee guide-tree generation is packaged; no separate phylogenetic engine is bundled.

`tool-sources` exports matching source archives, distro patches and checksums.
Runtime package/version inventory and package license notices are retained. Distro
transitive packages are not snapshot-pinned yet, so the image is not bit-reproducible.
Full license/security review and the existing npm audit findings remain release gates.
No image has been published to a registry.

### Implemented T-Coffee Increment

- `POST /api/alignments` accepts a UUID request ID and 2-20 same-kind sequences.
  Limits: 2,000 residues per sequence, 10,000 total and 256 KiB HTTP body. The initial
  supported alphabets are ungapped A/C/G/T DNA and the standard 20 protein letters.
  Lowercase is normalized; ambiguity, gaps, U and stops are rejected, never stripped.
- `TcoffeeService` fixes the offline method list, one core and generated FASTA IDs.
  Output rows are checked for IDs, count, length and exact ungapped residues, then
  restored to input order/names. Consensus coloring uses exact non-gap matches.
  MSA score is null. Tool/version/method and source IDs/names/SHA-256 digests survive
  workspace persistence and sharing. No source annotations are projected into MSA coordinates.
- `ExternalToolRunner` isolates cwd/HOME/temp variables, strips inherited credentials,
  drains both pipes and writes stdin concurrently. Default production deadline is two
  minutes, combined diagnostics/result cap 10 MiB and scratch cap 32 MB/2,000 paths.
  It checks symlinks, terminates tracked processes on all terminal paths and removes
  work directories. Startup removes sufficiently old, owned, unlocked leftovers;
  Docker tmpfs also disappears with container recreation.
- Flyway V2 adds durable inputs, timestamps, lease tokens, expiry and error codes.
  JDBC transactions serialize admission/claiming through a database guard row; no
  transaction is held during execution. One active worker, eight outstanding jobs,
  15-second leases refreshed every second, 24-hour retention. Queued jobs resume;
  abandoned running attempts fail with `INTERRUPTED` and require an explicit retry.
  Cancelled executions retain their slot until the worker stops or its lease expires.
- HTTP contracts: 202 accepted, 400 invalid input, 413 excessive input, 503 unavailable/
  full queue, 429 per-client submission limit, 409 reused ID with different input.
  Default rate limit is 10 new submissions per minute per remote address per instance;
  this is a bounded in-memory limiter, not a distributed abuse-control solution.
  GET/DELETE `/api/jobs/{id}` poll/cancel; no public job listing exists.
- `toolJobStore` persists pending capabilities and input snapshots separately under
  the local/shared draft key, with backoff and refresh recovery. Successful results
  insert once below the first source at submission; deleted sources require explicit
  insertion. Requests retain the submitted sequence version even after source editing.
  The new UI lives in `TcoffeeTool.svelte`, outside the main workspace component.

The later phase descriptions remain the target design, not a claim that all release
gates are satisfied. Binary output support, richer internal
diagnostics and freezing execution options/version at admission (rather than recording
the version actually used in the result) remain future runner/job improvements.

### Implemented Primer3 Increment

- `POST /api/primers` accepts a UUID, one DNA source and validated settings; it returns
  a durable job with HTTP 202. Limits: 40-20,000 ungapped A/C/G/T/N bases, 1-10 pairs,
  256 KiB request body. Primers/probes cannot contain ambiguous bases.
- The PCR dialog supports an optional selected target region, product-size range,
  primer length/Tm/GC constraints, reaction concentrations and an optional internal
  probe with separate length/Tm/GC constraints. Probe generation is opt-in.
- `PrimerDesignService` serializes allowlisted Boulder tags, uses the configured
  thermodynamic directory and validates returned sequences/coordinates against the
  source. API coordinates are zero-based half-open; UI positions are one-based.
  Reverse primers are returned in synthesis orientation, not as the forward template
  substring. No acceptable pairs is a successful result with diagnostic explanations.
- Primer3 shares T-Coffee's queue, leases, cancellation, retention and rate limits.
  Both tools create an immediate workspace placeholder before the POST response,
  show submitting/queued/running state, and replace it with the completed result.
  Failed/cancelled cards offer retry/dismiss; refresh preserves pending jobs.
- The additive `primerDesigns` project collection stores source, settings, version,
  pairs and explanations. Existing projects without this collection remain valid.
  Designs can be saved/shared/printed even if their source card has been deleted.
  Pair FASTA export, full-project oligo export, copying and adding oligos as ordinary
  DNA cards use 5'-to-3' oligo sequences, including internal probes.
- Genome-wide specificity, overhangs, forced/excluded primer regions and dye/quencher
  modifications are not implemented. This is not full legacy PCR feature parity.

### Implemented Guide-Tree Increment

- Per the 2026-10-16 scope decision, use T-Coffee's alignment guide tree rather than
  introducing FastTree. The UI action is **Analyze Alignment > Guide tree (T-Coffee)**.
  The legacy `muscle -maketree` action and binary-only `phylTree` HTML/SVG generator
  remain isolated; the modern workflow does not call them.
- `POST /api/trees` accepts an alignment snapshot and UUID, then uses the existing
  durable job queue. Inputs have 3-20 rows, at most 10,000 aligned columns, 2,000
  ungapped residues per row and 10,000 total. The supported alphabets match MSA.
  The pinned T-Coffee version writes no guide-tree file for a two-row alignment;
  these requests are rejected before enqueueing instead of failing after execution.
- `GuideTreeService` removes alignment gaps and recomputes T-Coffee's NJ guide tree
  using the established offline pairwise methods, one core and explicit `-newtree`.
  This is not a distance tree inferred from the current alignment columns, nor a
  cached original MSA artifact. Source alignment content is not changed.
- Forester 1.039 parses bounded Newick output using generated leaf IDs. Validate
  finite branch lengths, connected topology and exact leaf membership, then restore
  original names (including duplicates) with source indices and sequence hashes.
  The API returns flat typed nodes, never generated HTML or raw SVG.
- Tree cards support collapse/reorder/delete, browser recovery, sharing, print,
  standalone SVG and quoted Newick downloads. Pure layout functions feed Svelte SVG:
  rectangular branches, leaf markers, wrapped labels, branch-length or topology view.
  Zero-distance trees use topology; negative lengths are drawn at zero but retained
  numerically and in Newick. The display root has no biological meaning, and guide
  distances are not labelled as substitutions, time or bootstrap support.
- An additive optional `trees` project collection preserves older workspaces; no
  SQL schema change is needed. Forester's optional chart/PDF dependencies are excluded.
  Its LGPL-2.1-or-later notices/source must accompany redistribution; see the packaged
  `META-INF/FORESTER-NOTICE.txt`. Public-release licensing/security gates still apply.

### Target Deployment

Recommended first release: **one application image, two Compose services**.

```text
Browser -> HTTPS reverse proxy -> genewarrior:8080 -> PostgreSQL
                                  |
                                  +-- compiled Svelte assets + Spring JSON API
                                  +-- database-backed queue, bounded executor
                                  +-- T-Coffee / Primer3 / tree engine
```

- Spring serves the frontend at `/` and APIs on the same origin. Node/Vite and Gradle
  are build tools only; no Vite server or separate frontend container in production.
- PostgreSQL stores shares, job inputs/results and durable generated artifacts, with
  a named data volume. Keep H2 for non-container local development and tests.
- CLI tools run as subprocesses in the application container, not via `docker run`
  per request. Never mount the Docker socket or require privileged containers.
- Start with one application replica and one external job at a time. Set independent
  queue, thread, memory, output and time limits. Do not assume the historical small
  hosting instance can run T-Coffee; measure before setting deployment requirements.
- This shares a failure/security boundary between Java and the CLI tools. A native
  crash or memory exhaustion can affect the API. If measurements or public-service
  isolation requirements demand it, split execution into a separate worker service
  using the same pinned image and database queue; no broker is needed initially.
- No runtime installation or network-dependent sequence analysis. Restrict outbound
  access at deployment where practical. Existing browser-initiated NCBI BLAST remains
  an explicit external action; do not bundle BLAST databases for that feature.

### Tool Inventory

| Feature                                                        | Proposed implementation                   | Required runtime content                                                                             |
|----------------------------------------------------------------|-------------------------------------------|------------------------------------------------------------------------------------------------------|
| Import, transforms, translation, pairwise alignment, consensus | Existing Java/TypeScript logic            | Java runtime; no additional scientific executable                                                    |
| Multiple alignment                                             | T-Coffee, offline standard configuration  | `t_coffee`, its required libraries/data and only dependencies proven necessary for the selected mode |
| Primer design                                                  | Primer3 CLI                               | `primer3_core` plus matching `primer3_config` thermodynamic parameter files                          |
| Sequence logos                                                 | Implemented: Java statistics + Svelte SVG | Java runtime and bundled, licensed glyph outlines; no external process                               |
| Alignment guide trees                                          | T-Coffee NJ guide tree (implemented)      | Existing T-Coffee binary; Forester parser and native SVG renderer                                    |
| NCBI BLAST                                                     | Existing browser form submission          | No local executable or databases                                                                     |

T-Coffee's standard and meta/template modes have different dependency requirements.
M-Coffee can invoke MUSCLE and other aligners; template modes can contact remote
services. Use an explicitly tested offline configuration, not the installer's
`install all` bundle. The release must work with no MUSCLE executable available.
[T-Coffee installation documentation](https://tcoffee.readthedocs.io/en/latest/tcoffee_installation.html)

Primer3's thermodynamic data must be installed alongside the executable and configured
explicitly, not found through a developer's working directory.
[Primer3 parameter-path documentation](https://primer3.org/manual.html#PRIMER_THERMODYNAMIC_PARAMETERS_PATH)

The native logo implementation supersedes the earlier WebLogo packaging proposal.
Its statistical scope is deliberately smaller than WebLogo's Bayesian estimation;
do not claim WebLogo-equivalent results. The legacy UI still has its old WebLogo
process call until that UI is retired, but the modern API/configuration no longer does.
[WebLogo statistical background](https://weblogo.threeplusone.com/manual.html)

The legacy tree action uses `muscle -maketree`; replacing the alignment button alone
does not remove MUSCLE. T-Coffee's `.dnd` output is a guide tree, not a replacement
phylogenetic analysis. FastTree is a reasonable separate candidate for DNA/protein
alignments, but it changes the inference method: expose model and support semantics,
and do not promise identical legacy trees. Its Newick output can contain multifurcations
and internal support labels, which the old binary-only `phylTree` parser cannot safely
handle. Use a tested parser and frontend renderer instead of legacy generated HTML.
[T-Coffee guide-tree warning](https://tcoffee.readthedocs.io/en/latest/tcoffee_main_documentation.html#building-multiple-sequence-alignments),
[FastTree output documentation](https://morgannprice.github.io/fasttree/)

### Phase 1: Reproducible Toolchain

**Deliverables:** tool-version manifest, container tool-build/runtime stages and real
smoke-test fixtures. Build this early to discover native dependency problems, without
claiming the application deployment is ready.

- Choose maintained Debian/Ubuntu-based, glibc-compatible build/runtime images and
  a Java 25 runtime. Pin image digests and tool release archives/commits with checksums;
  do not build from moving branches or download `Latest` at startup.
- Prefer verified distro packages where suitable; otherwise compile pinned upstream
  sources in a builder stage. Inspect T-Coffee's installation scripts for implicit
  downloads or optional plugin installation. Verify native shared-library dependencies
  against the runtime base. No Python environment is needed for native logos.
- Initially target `linux/amd64`. Add `linux/arm64` only after native builds and real
  fixture tests pass; emulation alone does not establish supported performance.
- Run tools as the intended non-root user with a restricted PATH, fresh writable HOME
  and temporary directory, without internet or host-installed scientific software.
- Smoke-test DNA and protein T-Coffee alignments, an actual Primer3 design requiring
  thermodynamic files and a parseable tree. Native logo rendering has separate browser
  tests and does not belong in the external-tool smoke tests.
- Produce a dependency/license inventory, including bundled code, datasets, fonts and
  native transitive dependencies. Preserve notices and arrange corresponding
  source distribution where required. Review exact pinned T-Coffee/FastTree licensing;
  do not assume GeneWarrior's MIT license covers its bundled tools. Primer3 publishes
  GPLv2 terms. License review is a
  release gate, not a conclusion that putting executables in separate processes settles
  all obligations.
  [Primer3 license](https://github.com/primer3-org/primer3/blob/main/LICENSE)

**Exit gate:** clean, repeatable tool-runtime build; every fixture succeeds offline;
versions and measured time/peak memory are recorded; no MUSCLE dependency remains in
the selected T-Coffee execution path. Exact release pins are chosen here, not guessed
from documentation version banners.

### Phase 2: Safe Process Execution

**Deliverables:** revised `ExternalToolRunner`, validated configuration and process tests.

- Support a per-job working directory, controlled environment and file inputs/outputs.
  Keep diagnostics separate from result files; native logos do not use this runner.
- Write stdin and drain stdout/stderr concurrently. Start the deadline before I/O;
  close streams and terminate/reap the process and descendants on timeout, cancellation,
  interruption or output overflow. Test children that ignore graceful termination.
- Keep all application process launching here, including availability probes. Use
  argument arrays and server-generated filenames/sequence identifiers. Accept typed,
  allowlisted options only; no shell command, executable path or arbitrary tool flags
  from the client. Do not rely on external `timeout`/`nice` wrappers for correctness.
- Limit stdout/stderr, files, total scratch bytes, alignment dimensions and job input
  size. Poll disk usage while running and use bounded scratch storage; checking file
  size only after completion is insufficient. Reject symlink/path escapes on outputs.
- Give each job isolated HOME/TMPDIR/cache locations and a minimal environment without
  database credentials. Cleanup in all terminal paths and on startup after crashes.
  Cgroup memory/CPU/PID limits complement, but do not replace, application limits.
- Replace `genewarrior.tools.muscle.path` with `genewarrior.tools.tcoffee.path`; add
  Primer3 parameter-directory and tree-engine paths, per-tool timeouts/thread limits,
  queue capacity and scratch limits. Preserve simple dedicated tool properties.
- Record exit code/version/duration and bounded diagnostics. Redact sequence data,
  raw tool output, credentials and internal paths from public errors and routine logs.

**Exit gate:** tests prove no deadlock on full stdout/stderr pipes or blocked stdin,
binary fidelity, timeout/cancellation cleanup, output limits and isolated concurrent
work directories. Missing/broken tools are detected by real smoke probes, not only
`Files.isExecutable`.

### Phase 3: Durable Jobs And Frontend Polling

**Deliverables:** new Flyway migrations, job executor, typed job APIs and frontend job UI.

- Persist validated input snapshots before returning HTTP 202. Existing endpoints
  create jobs without request bodies, and `tool_job` has no input column; both must
  change. Add started/heartbeat/expiry timestamps, tool version/options, source IDs,
  request idempotency key and structured result/error fields. Do not edit applied V1.
- Use atomic database claims and short transactions, never hold a transaction while
  a process runs. Poll committed `QUEUED` rows so a crash between commit and dispatch
  does not lose work. Start with a bounded executor and a bounded database queue.
- Define `QUEUED -> RUNNING -> SUCCEEDED | FAILED | CANCELLED`; represent timeout as a
  failure with a specific code. Make cancellation race-safe. On restart, reclaim only
  abandoned/expired execution leases, mark interrupted attempts failed and offer an
  explicit retry; retain queued inputs and already completed results.
- Validate before enqueueing: 400 invalid biology/options, 413 excessive input, 503
  unavailable tool or full global queue, 429 per-client rate limit. Expose job polling
  and cancellation plus a small capabilities endpoint for tool availability/versions.
- Use unguessable job capabilities and avoid public job listings. Set submission rates,
  queue limits, result retention and expiry for the unauthenticated service. A capability
  URL is not authentication; document its privacy limits and redact it from logs.
- Wire `toolJobStore`: queued/running/error/cancel/retry states, polling with backoff,
  and refresh recovery. Persist pending job IDs with the originating local/shared draft,
  not in immutable public shares. Do not insert results into a different open project.
- Insert each completed result once, immediately below its originating card (for MSA,
  the first source in workspace order at submission). Preserve source names and order
  in the request snapshot. If a source was deleted, offer explicit result insertion
  instead of resurrecting it; if edited, keep provenance tied to the submitted version.

**Exit gate:** fake-tool tests cover lifecycle, queue saturation, duplicate submissions,
cancellation/completion races, restart recovery, polling across refresh and exactly-once
workspace insertion. PostgreSQL tests exercise claiming and migrations, not just H2.

### Phase 4: Replace MUSCLE With T-Coffee

**Deliverable:** the first complete external-tool workflow, using the tested runtime.

- Add `TcoffeeAlignmentService` behind validated `POST /api/alignments`; return 202
  and an alignment job. Keep `/api/alignments/pairwise` and its Java algorithms unchanged.
- Accept at least two same-kind sequences with limits on sequence count, individual
  length and total residues. Benchmark DNA/protein fixtures before setting production
  caps; the existing universal 20-second timeout is not an assumed MSA budget.
- Write FASTA using generated IDs such as `seq0001`, mapped back to original names/IDs.
  Explicitly choose DNA/protein mode and aligned FASTA output supported by the pinned
  release. Freeze the method list/options proven in phase 1; do not expose a free-form
  T-Coffee command or switch to M-Coffee/remote modes automatically.
- Define and test handling of input gaps, ambiguous symbols, U/T and protein stops.
  Reject unsupported inputs with useful errors or document explicit normalization;
  never silently remove residues. Verify output IDs/count, equal row lengths and
  ungapped residue preservation, then restore submission order.
- Return structured `AlignmentDto` data. Compute consensus coloring with an explicit
  policy (initially exact non-gap matches, consistent with pairwise behavior); do not
  confuse T-Coffee confidence scores with conserved columns or pairwise scores.
  Make the existing required integer `score` optional/tool-specific across Java,
  TypeScript, validation and persistence rather than inventing a pairwise score for MSA.
- Rename the UI action to T-Coffee, enable it only for valid selected inputs and a
  healthy capability, and show progress through the shared job UI. Preserve marking,
  searching, copying, sharing and CLUSTAL export on the generated alignment.
- New MSA cards start with no inherited marks: source coordinates are not alignment
  coordinates. Annotation projection, if later requested, needs an explicit gap mapping.
- Remove modern MUSCLE configuration and disable the legacy execution endpoint in the
  production profile. No hidden fallback to MUSCLE. Finish legacy removal in phase 6.

**Exit gate:** real DNA/protein alignment tests in Docker pass; names/order/residues,
duplicate names and unsupported inputs are checked; browser submission through sharing
and export works; results survive restart. Equivalent MUSCLE output is not a criterion.

### Phase 5: Remaining Tools And Result Persistence

Implement one dedicated service/workflow at a time, sharing only runner/job plumbing.

1. **Consensus extraction: complete.** `AlignmentService` uses the legacy column-voting
   rules: each symbol (including ambiguity symbols) contributes one literal vote.
   A uniquely most frequent non-gap symbol wins; two tied DNA symbols are combined
   using the existing IUPAC lookup, three or more tied symbols produce `N`, and protein
   ties produce `X`. Gaps remove a column only when they strictly outnumber the most
   frequent non-gap symbol; gap/residue ties retain the residue decision. Output is
   ungapped, uppercase, with DNA `U` normalized to `T`; protein stops remain eligible
   symbols. Empty results return HTTP 400. Input limits are 100,000 columns, 10,000
   rows and 2,000,000 cells (HTTP 413); malformed or unequal rows return HTTP 400.
   Each selected alignment produces a named standalone sequence immediately below
   its source, without copying alignment-coordinate annotations. Results use normal
   autosave, sharing and FASTA export. Unit/API tests cover the rules and validation;
   browser tests cover multi-selection, placement, reload, sharing, export and errors.
2. **Primer3: initial scope complete.** Typed target/product-size/temperature constraints,
   explicit parameter directory, Boulder-IO serialization/parsing and asynchronous
   designs are implemented. Results include left/right primers, optional internal
   probes, coordinates, sequences, Tm, GC, penalties and explanations. Tests distinguish
   no acceptable primers from failures and verify right-primer strand/coordinates.
   Primer selection is not a substitute for genome-wide specificity checking.
3. **Sequence logos: initial scope complete.** Java statistics and Svelte SVG replace
   WebLogo. Frequency/information modes, coverage, SVG export, printing and persistence
   are implemented. Advanced background models, corrections and confidence intervals
   remain separate enhancements, not blockers for external-tool packaging.
4. **Guide trees: initial scope complete.** T-Coffee guide trees are parsed and rendered
   as SVG, with Newick export, source mapping and persistence. This supersedes the
   initial FastTree implementation proposal for this wave. Model-based phylogenetic
   inference would be a separate future tool with explicit model/support semantics.

The current workspace persists sequences, alignments, native logos, primer designs
and guide trees through typed DTOs, validation, explicit entry order, autosave and sharing.
Version the workspace payload and explicitly migrate current drafts/shares, rather
than silently dropping them when adding new entry types. Persist provenance and options.

Native logos store their statistics and options directly in the project JSON and render
locally; no image artifact table, expiration coupling or external image fetch is needed.
If future tools require durable binary artifacts, add bounded artifact storage and
reference-aware cleanup then. Never rely on transient job scratch files for shared results.

**Exit gate:** each tool works with real binaries, has failure/limit tests and usable
result controls, and its results survive save/load, sharing and application restart.

### Phase 6: Complete Application Image And Compose

**Deliverables:** production Dockerfile, `.dockerignore`, `compose.yaml`, example
configuration/secrets, production Spring profile and deployment documentation.

- Use separate Node frontend, Java/Gradle and native-tool builder stages. Copy compiled
  frontend assets into generated classpath static resources before `bootJar`; do not
  overwrite tracked legacy assets. Runtime contains the JRE, application JAR, tested
  CLI tools/data and required shared libraries, but no build
  caches, node_modules, compiler or source checkout. Follow Docker's multi-stage pattern.
  [Docker multi-stage builds](https://docs.docker.com/build/building/multi-stage/)
- Remove/profile-gate `MainController`'s `/` and legacy tool routes so `/` serves Svelte.
  Test `/?project=...`, fresh-link loading and same-origin API/assets without Vite.
  Unknown `/api/...` and missing assets must remain real errors, not SPA HTML.
- Consolidate configuration: most defaults currently live in root `application.properties`,
  not a packaged main resource. Put production defaults on the classpath, remove legacy
  file-based template/software paths and use explicit environment/secret bindings.
  Never activate the local/H2 profile in the release image.
- Compose starts the app and PostgreSQL with a named database volume, healthchecks,
  restart policies, bounded logs and explicit resource limits. Use database health to
  order initial startup and retry transient connections in the app; startup ordering is
  not a recovery strategy. Do not publish the database port by default.
  [Compose startup and shutdown ordering](https://docs.docker.com/compose/how-tos/startup-order/)
- Run non-root with read-only root filesystem, writable bounded scratch space,
  dropped capabilities and no-new-privileges. Use an init/reaping strategy and graceful
  shutdown that stops new claims and terminates running process trees before exit.
  Set JVM heap below the container limit to leave measured headroom for native tools.
- Add Actuator liveness/readiness plus cached tool capability probes. Do not execute
  expensive fixtures on each health request. Production readiness requires the DB and
  declared required tools; liveness must not restart the app for a transient DB outage.
  Expose only minimal health information publicly, not environment/config endpoints.
- Document TLS/reverse proxy, trusted forwarded headers, body/time limits and rate
  limiting. Keep local-only binding as the default Compose exposure. Store credentials
  outside images/source; configure Spring to read mounted secrets where used.
- Remove legacy controllers, templates, process calls, Gson and unused dependencies
  once modern feature parity and tests pass. Tighten CSP for Svelte/assets and explicit
  NCBI form submission; do not retain unsafe inline permissions merely for dead code.

**Exit gate:** a clean machine with Docker/Compose can build and run the full app
without host Java, Node, Python or scientific tools. No external downloads occur on
startup or during ordinary analysis; PostgreSQL shares/artifacts survive recreation.

### Phase 7: Release Gates And Operations

- CI runs Java tests, frontend checks/unit tests/build, browser tests and real tool
  fixtures against the final image plus PostgreSQL. Mocks remain appropriate for fast
  normal service tests but are not sufficient for release verification.
- Exercise simultaneous submissions, long/invalid inputs, malformed/oversized output,
  full scratch disk, blocked subprocess I/O, cancellation, SIGTERM and forced restart.
  Verify no orphan processes, stuck RUNNING jobs, duplicate cards or cross-job files.
- Benchmark an agreed representative and maximum accepted workload on deployment-sized
  hardware. Record runtime, peak RAM, scratch usage, image size and API responsiveness;
  adjust caps/concurrency or adopt a separate worker before release if necessary.
- Test database migrations from the current schema, backup/restore, immutable shared
  artifacts, expiry/cleanup and disk growth. Document database-compatible image rollback;
  do not assume rolling back the image reverses a Flyway migration.
- Scan Java/npm/OS dependencies and images, address the recorded npm advisories,
  produce an SBOM and retain tool version/provenance/license/source records. Publish
  versioned image tags/digests and a reproducible dependency update procedure.
- Release only when the chosen tool versions, supported CPU architecture, resource
  budget, retention policy and licensing review are recorded. No claim of ARM support
  or numerical equivalence across tool upgrades without real tests.

**Next implementation slice:** address the public-release hardening gates and expand
interaction coverage. Model-based phylogenetic inference remains optional future work;
the requested T-Coffee guide-tree workflow is implemented.

## Migration Phase Summary

| Original phase         | Current assessment                                                                     |
|------------------------|----------------------------------------------------------------------------------------|
| Stabilize current app  | Partial: tests/configuration/migrations exist; legacy logging and process calls remain |
| Clean backend API      | Core operations implemented; advanced tools and validation incomplete                  |
| Extract domain library | Algorithms retained and partly wrapped; broader cleanup/testing pending                |
| Rewrite frontend       | Core workspace, persistence and exports implemented; advanced tools incomplete         |
| Retire legacy UI       | Source removal pending; production serves only the modern UI                           |

The architecture choice is settled: **Spring Boot backend plus Svelte frontend**.
The remaining work is completing and consolidating that implementation.

