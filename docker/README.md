# Docker Deployment

The modern workspace and APIs run from one image, backed by PostgreSQL. T-Coffee
multiple alignment and Primer3 design are enabled through persistent asynchronous jobs.
Primer3 supports PCR pairs with an optional internal probe.
T-Coffee guide trees are rendered as SVG; no separate phylogenetic inference engine
is packaged. Native sequence logos need no external binary.

## Start

From the repository root, with Docker Engine and Compose installed:

```sh
sh docker/init-secrets.sh
docker compose up -d --build --wait
docker compose ps
```

Open **http://localhost:8081**. The default bind is loopback only; the existing Vite
development server on 5173 and backend on 8080 are unaffected. Changing the origin
creates a separate browser draft. Existing H2 shares are not copied into PostgreSQL.

No host Java, Node, Python or scientific tools are needed to build or run the app.
The first build downloads images, Java/npm dependencies and distro packages; startup
and local biological operations require no downloads. The build runs backend tests,
frontend type checks/unit tests/build, and offline scientific fixtures.

Optional Compose settings are listed in `.env.example`. Set `GENEWARRIOR_PORT` or
`GENEWARRIOR_BIND_ADDRESS` in the environment or a local, ignored `.env` file.
Only `linux/amd64` is currently verified. ARM support requires a separate test pass.

## Credentials And Storage

`docker/init-secrets.sh` creates a random database password without printing it or
overwriting an existing one. `.secrets/` is ignored by Git and Docker. Its host
directory is private (0700); the password file is readable inside both non-root
containers via a read-only Compose secret mount. Spring reads it through
`configtree:/run/secrets/`; PostgreSQL uses `POSTGRES_PASSWORD_FILE`.

Compose secrets on a standalone Docker engine are file mounts, not an encrypted
secret manager. Protect and back up the host credential. Do not edit the file to
rotate an existing database password: PostgreSQL initialization variables only
apply to an empty data directory. Coordinate a database password change and app
restart for rotation.

`genewarrior_postgres-data` is the default named database volume. Container recreation
and `docker compose down` preserve it. Removing the volume destroys shared projects
and job records. Back it up and test restoration before hosting real data; a typical
logical backup is `docker compose exec -T db pg_dump -U genewarrior -d genewarrior`.
Keep database dumps private. No database port is published.
Once a Flyway migration has been applied, leave its contents (including formatting)
unchanged; subsequent schema changes belong in a new migration. Never delete the
database volume to work around a migration validation error.

## Runtime Boundaries

- Spring serves compiled assets from `classpath:/webapp/`, with APIs on the same
  origin. `/?project=...` works directly, without Vite or an SPA catch-all. Missing
  assets/API routes remain 404.
- Legacy controllers/services require the explicit `legacy` profile and cannot run
  with `production`. Legacy static assets/templates are excluded from the release
  JAR. The unused Spring Data REST dependency was removed to avoid repository routes.
- The app runs as UID/GID 10001; PostgreSQL runs as its `postgres` user. Both use
  read-only roots, dropped capabilities, no-new-privileges and bounded tmpfs/logs.
- The app has 2 GiB RAM, two CPUs, 256 PIDs and 256 MiB scratch in Compose. JVM heap
  is capped at 50% of container RAM. These are starting limits, not capacity claims.
  `tini -g` forwards signals/reaps children; Spring has graceful shutdown.
- `/actuator/health/liveness` tracks application lifecycle; readiness also checks
  PostgreSQL. Responses expose status only. Tool capability is separate from general
  readiness: `/api/tools/tcoffee` and `/api/tools/primer3` expose startup probe results
  and limits. Primer3's probe exercises its thermodynamic parameter files too.
- CSP permits same-origin scripts, inline styles needed by sequence rendering, and
  browser form submissions to NCBI BLAST. Forwarded headers are ignored by default.
  Public hosting needs HTTPS, a trusted reverse proxy, body/time limits and rate
  limits. Configure forwarded-header trust only for a controlled proxy topology.

This is **not yet a public-release approval**. The unauthenticated app still needs
consistent computational limits across all APIs, public abuse controls, dependency/security
review and tested backup/restore. Existing npm audit findings (including Svelte and
build tooling) remain; npm/Node/Vite are absent from the final runtime, but that does
not by itself clear the advisories affecting bundled frontend code.

## Scientific Tool Contract

| Tool     | Pinned Ubuntu Noble package                    | Tested invocation/data                                                                                             |
|----------|------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| T-Coffee | `13.45.0.4846264+really13.41.0.28bdc39+dfsg-1` | Reports upstream `13.41.0.28bdc39`; explicit `slow_pair,lalign_id_pair`, DNA/protein type, one core, aligned FASTA |
| Primer3  | `2.6.1-4`                                      | `/usr/bin/primer3_core`, `/etc/primer3_config/`, thermodynamic oligo and template alignment enabled                |

The T-Coffee package version includes `really`: it is **not upstream 13.45**. These
are distro-managed baseline versions, not a claim to use the newest upstream release.
No optional/recommended aligners, MUSCLE, M-Coffee, remote templates, BLAST databases,
WebLogo or Python environments are installed. T-Coffee needs `TMP_4_TCOFFEE`, not just
`TMPDIR`; the smoke test gives it an isolated working directory, HOME and temp space.
The application runner does the same, strips inherited credentials and bounds I/O,
execution time and scratch usage. It removes completed scratch and old unlocked
workspaces at startup. Container tmpfs is not durable job storage; inputs/results
live in PostgreSQL.

The fixtures verify row identifiers, equal aligned lengths and ungapped DNA/protein
residue preservation, plus a real thermodynamic primer pair and product-size range.
On the development amd64 machine, the tiny T-Coffee fixtures took approximately
0.5-0.8 seconds and 475 MiB peak RSS; Primer3 took approximately 0.01 seconds and
6 MiB. These are installation checks, not production benchmarks. Plan one tool job
at a time until representative workloads are measured; this is enforced by the database queue.

## Alignment Jobs

Select 2-20 DNA cards or 2-20 protein cards, then choose **Create Alignment > T-Coffee**.
Only ungapped A/C/G/T DNA and standard 20-letter proteins are enabled initially.
Ambiguities, U, stops and input gaps are rejected explicitly. Maximum length is 2,000
residues per sequence and 10,000 combined. Duplicate names are supported.

An immediate placeholder card appears below the first source, showing submission,
queue and execution status. Jobs survive browser refresh. Cancel and Retry operate on saved input snapshots.
Results insert below the first source in workspace order at submission. If that
source was deleted, use **Add alignment** to append the completed result explicitly.
Pending job IDs are stored with the originating browser draft, not in shared projects.
Sharing and CLUSTAL export include completed alignments; shared provenance records
the tool/version/method and source hashes, not job capabilities.

## Primer Design

Select one DNA template, then **PCR > Primer design**. Supported templates contain
40-20,000 ungapped A/C/G/T/N bases. A selected subsequence initializes the optional
target region. Set product-size range, primer length/Tm/GC constraints and reaction
conditions; enable **Include internal probe** for separate probe constraints.
Request 1-10 pairs. Primers and probes themselves cannot contain ambiguous bases.

Submission creates the same immediate placeholder as alignment. Results include
forward/reverse primer and optional probe sequences in 5'-to-3' synthesis orientation,
a template map, product size, coordinates, Tm, GC and penalties. An empty result is a
completed design with explanations, not a failed job. Designs retain source/settings/
version, survive refresh and sharing, and support oligo FASTA export, printing and
adding oligos as ordinary DNA entries. Transient job capabilities are not shared.

The UI uses one-based inclusive positions; the API uses zero-based half-open ranges.
Genome-wide specificity, overhangs, forced/excluded primers and probe dye/quencher
modifications are not included. Review designs before experimental use.

## Guide Trees

Select an alignment and choose **Analyze Alignment > Guide tree (T-Coffee)**.
Each selected alignment gets a pending card immediately below it. The worker removes
gaps and recomputes T-Coffee's neighbor-joining guide tree using the same offline
pairwise methods as MSA, with explicit `-newtree`. Existing alignment columns are not
modified or used as an evolutionary distance matrix; this is not a cached MSA artifact.
Limits are 3-20 rows, 10,000 alignment columns, 2,000 ungapped residues per row and
10,000 combined, using the same supported DNA/protein alphabets as alignment.
The pinned tool does not write guide-tree output for only two rows; pairwise
alignments therefore cannot use this action.

Results preserve duplicate names through indexed leaf IDs. Cards support a
branch-length view or topology-only view, wrapped labels, SVG/Newick downloads,
printing and complete workspace persistence/sharing. Zero-distance trees fall back
to topology. Negative branch lengths are drawn at zero and preserved in Newick.
Guide distances are not evolutionary time or bootstrap support; the displayed root
is arbitrary. T-Coffee explicitly distinguishes its guide trees from phylogenetic
inference
([tool documentation](https://tcoffee.readthedocs.io/en/stable/tcoffee_technical_documentation.html#librairies-trees)).

Forester 1.039 parses the bounded Newick output; the browser only receives validated
flat nodes and renders SVG without raw HTML insertion. Its optional chart/PDF
dependencies are excluded. See `META-INF/FORESTER-NOTICE.txt` in the application
archive for the LGPL-2.1-or-later notice and corresponding source location. Retain
the library's source/license when distributing the application; the tool source-export
target alone does not export Java dependencies.

## Shared Job Limits

Defaults (Spring environment/property overrides are supported):

| Property                                  | Default                                                                             |
|-------------------------------------------|-------------------------------------------------------------------------------------|
| `genewarrior.tools.default-timeout`       | 2 minutes in production; 20 seconds locally                                         |
| `genewarrior.tools.max-input-bytes`       | 5 MiB; alignment, primer and tree endpoints additionally cap HTTP bodies at 256 KiB |
| `genewarrior.tools.max-output-bytes`      | 10 MiB combined stdout/stderr/result                                                |
| `genewarrior.tools.max-scratch-bytes`     | 32,000,000 bytes, plus a fixed 2,000-path cap                                       |
| `genewarrior.jobs.queue-capacity`         | 8 outstanding jobs, one execution at a time                                         |
| `genewarrior.jobs.retention`              | `PT24H` from submission                                                             |
| `genewarrior.jobs.submissions-per-minute` | 10 per remote address per application instance                                      |
| `genewarrior.jobs.worker-enabled`         | true; disable only for maintenance/tests                                            |

Job GET/DELETE endpoints use UUID capabilities. Anyone holding one can read its
result or cancel it; these are not authentication. Keep job URLs out of access logs
and analytics. The rate limiter is in memory, resets on restart and does not trust
forwarded headers. Configure public proxy limits before exposing the service.

Queued inputs remain durable across restart. A running attempt whose 15-second lease
expires becomes FAILED/INTERRUPTED instead of silently rerunning. Cancellation wins
against late completion. Expired records are removed by the queue poller. No worker
means no retention cleanup. Process monitoring tracks observed descendants; detached
process containment and upper-bound workload benchmarks remain public-release gates.

Reference contracts: [T-Coffee methods](https://tcoffee.readthedocs.io/en/latest/tcoffee_main_documentation.html),
[Primer3 parameter files](https://primer3.org/manual.html#PRIMER_THERMODYNAMIC_PARAMETERS_PATH).

## Verification

Test the final image's tools without networking, host tools or writable root:

```sh
docker run --rm --network none --read-only \
  --tmpfs /tmp:size=256m,mode=1777,noexec,nosuid,nodev \
  --cap-drop ALL --security-opt no-new-privileges \
  --memory 1g --cpus 1 --pids-limit 128 \
  --entrypoint /opt/genewarrior/smoke-tools genewarrior:local
```

For development verification with host Node installed:

```sh
node docker/smoke-app.mjs
GENEWARRIOR_FRONTEND_URL=http://127.0.0.1:8081 \
  PLAYWRIGHT_CHROMIUM_EXECUTABLE=/snap/bin/chromium npm --prefix frontend run test:e2e
```

The app smoke command prints a test share key. After `docker compose up -d
--force-recreate --wait`, run `node docker/smoke-app.mjs <that-share-key>` to verify
the same PostgreSQL snapshot survives recreation. Recreating the services interrupts
active requests. Use an appropriate Chromium path on other machines.

## Pins And Notices

Base-image digests are pinned in `Dockerfile` and `compose.yaml`; direct scientific
package versions are pinned in Docker build arguments. npm uses `package-lock.json`.
Runtime package inventory is `/opt/genewarrior/packages.tsv`. OS transitive packages
still come from the live signed Ubuntu repositories: rebuilds are version-controlled,
but **not fully bit-reproducible**. Before release, adopt a dated distro snapshot or
retained package mirror and dependency verification; refresh pins deliberately for
security fixes and rerun the offline/container/browser tests.

Package notices remain at `/usr/share/doc/t-coffee/copyright`,
`/usr/share/doc/primer3/copyright` and `/usr/share/common-licenses/`. The pinned distro
packages declare GPL-2-or-later; inspect their complete notices and source before
redistribution. The application license does not cover all bundled components.
Logo font notices ship in the frontend's `/licenses/` path.

The optional source-export target downloads the matching tool sources, distro
patches and `.dsc` metadata, with a SHA-256 inventory, without adding them to runtime:

```sh
docker build --target tool-sources --output type=local,dest=docker/source-archives .
```

Archive these alongside any distributed image and complete a full dependency/license
review, including the JRE, OS and Java/frontend libraries. Exporting the two tool
sources alone is not a blanket compliance claim.

## Next Integration Step

T-Coffee alignments/guide trees and Primer3 now have asynchronous workflows.
Representative workload benchmarks, dependency remediation and the other
public-release gates remain outstanding. Model-based phylogenetic inference would
be a separate future tool, not a reinterpretation of the current guide trees.
Specific tool-version requirements, ARM deployment or additional T-Coffee modes
should be agreed before extending this baseline.
