# GeneWarrior Modernization Recommendations

This document is based on a code review of the current Spring Boot backend, embedded Thymeleaf pages, jQuery frontend,
static assets, and the local Gradle/Java setup.

## Executive Summary

GeneWarrior still has a useful shape: a mostly stateless sequence manipulation tool, with optional persistence only for
shared projects. That is a good foundation. The main modernization need is not "make it newer"; it is separating the
concerns that are currently compressed into a few very large files.

The highest-value path is:

1. Keep Spring Boot as the backend, but turn it into a typed JSON API with focused services.
2. Replace the jQuery/jQuery UI frontend with a TypeScript app.
3. Move expensive external tools such as MUSCLE, Primer3, and WebLogo behind explicit job/executor services.
4. Add real tests around the sequence operations before rewriting behavior.
5. Treat sharing, input sanitization, and process execution as security-sensitive surfaces.

The project now builds with Java 25 and Gradle 9.3.0, but the code style is still pre-modern Java. Java 25 only helps
once the code is refactored into smaller typed units.

## Current Architecture

### Backend

- `MainController` serves pages and exposes a small number of POST endpoints.
- `DnaToolsService` is the core backend module and is currently around 2,100 lines.
- `DnaToolsService.processRequest(...)` dispatches all operations through a `type` request parameter.
- Most backend responses are manually assembled as DTO-like inner classes in `outputTypes`.
- JPA persistence is limited to shared projects through `ProjectEntity` and `ProjectEntityRepository`.
- Several operations shell out to external binaries:
    - `muscle`
    - `Primer3`
    - `weblogo`
    - Unix `timeout`
    - Unix `nice`

### Frontend

- Main UI is in `index.html`, `dnatools.js`, and `dnatools_svg.js`.
- `dnatools.js` is around 1,650 lines and uses global state arrays such as `sequenceEntry` and `selected`.
- `primer.html` and `translation.html` contain large embedded scripts and interact with parent dialogs through
  `window.parent`.
- The UI uses jQuery 1.11 and jQuery UI 1.10.
- HTML is frequently built through string concatenation and inserted with `.html()` / `.append()`.
- State is stored in `localStorage` as serialized frontend objects.

## Main Problems To Address

### 1. Single Backend Endpoint With String Dispatch

Current shape:

- `POST /dnatoolsServlet`
- form fields carry `type`, `text`, and operation-specific parameters
- backend switches on `type`

Problems:

- No typed request models.
- No validation annotations.
- No OpenAPI contract.
- Hard to test individual operations through HTTP.
- Frontend and backend are tightly coupled through ad hoc strings.
- Errors return as successful HTTP `200` responses containing `{ type: "error" }`.

Recommended shape:

- Replace the single endpoint with explicit JSON endpoints:
    - `POST /api/sequences/import`
    - `POST /api/sequences/reverse-complement`
    - `POST /api/sequences/translate`
    - `POST /api/sequences/find`
    - `POST /api/alignments/pairwise`
    - `POST /api/alignments/multiple`
    - `POST /api/alignments/consensus`
    - `POST /api/primers/design`
    - `POST /api/projects/share`
    - `GET /api/projects/{key}`

Use Spring MVC with Java records:

```java
public record ReverseComplementRequest(
        @NotBlank String sequence,
        boolean reverse,
        boolean complement,
        List<SequenceMark> marks
) {}
```

Return typed responses and use proper status codes:

- `200 OK` for successful operations
- `400 Bad Request` for invalid input
- `404 Not Found` for missing share keys
- `413 Payload Too Large` for over-limit sequences/projects
- `503 Service Unavailable` for missing external tools

### 2. `DnaToolsService` Does Too Much

`DnaToolsService` currently handles:

- request parameter extraction
- validation
- parsing FASTA-like input
- DNA/protein operations
- alignment operations
- external process execution
- database persistence
- JSON serialization
- HTML fragment construction
- error message formatting

Recommended split:

- `SequenceImportService`
- `SequenceTransformService`
- `TranslationService`
- `AlignmentService`
- `PrimerDesignService`
- `SequenceLogoService`
- `ProjectSharingService`
- `ExternalToolRunner`
- `SequenceResponseMapper`

This would let most operations be tested without Spring and without a database.

### 3. JSON Serialization Is Hand-Rolled Around Mutable Inner Classes

`outputTypes.java` defines nested mutable package-private fields for every response type. Gson serializes fields
directly.

Recommended update:

- Use Jackson, which Spring Boot already configures.
- Replace `outputTypes` with records or small DTO classes.
- Use a discriminated response model where needed.
- Avoid embedding HTML in JSON responses except for deliberately trusted generated output.

Example:

```java
public sealed interface SequenceResult permits DnaResult, ProteinResult, AlignmentResult, TextResult {}

public record DnaResult(
        String id,
        String name,
        String sequence,
        List<SequenceMark> marks,
        String info
) implements SequenceResult {}
```

### 4. External Process Execution Needs A Boundary

The app shells out directly from request handling methods. This is workable for a small tool, but it is a reliability
and security boundary.

Risks:

- Request threads block while external tools run.
- Tool paths are string-concatenated from `software.directory`.
- `timeout` and `nice` assume Linux userland.
- stdout/stderr handling is repeated.
- Missing tools return generic "Error occured".
- Large input can still tie up process slots.

Recommended update:

- Create one `ExternalToolRunner` with:
    - configured executable paths
    - startup validation
    - structured timeout handling
    - max input/output size
    - explicit exit-code mapping
    - logging with SLF4J
- Prefer absolute executable properties:
    - `genewarrior.tools.muscle.path`
    - `genewarrior.tools.primer3.path`
    - `genewarrior.tools.weblogo.path`
- Run long jobs asynchronously when useful:
    - simple option: Spring `TaskExecutor`
    - bigger option: queue table + worker
    - deployment option: separate worker container

For container deployment, package MUSCLE, Primer3, and WebLogo in the image rather than assuming
`/home/genewarrior/bin/`.

### 5. Input Validation And Security Need Attention

Important surfaces:

- shared project storage and retrieval
- generated HTML returned to the browser
- dynamic HTML insertion in jQuery
- downloadable filenames
- external process stdin
- root-level DB credentials in `application.properties`

Recommended updates:

- Add Bean Validation for every request DTO.
- Use length limits consistently and centrally.
- Sanitize filenames in `dnatoolsServletDownload`.
- Add a security header configuration:
    - Content Security Policy
    - `X-Content-Type-Options`
    - frame restrictions, except where deliberate
    - referrer policy
- Move credentials out of committed properties:
    - environment variables
    - Docker secrets
    - deployment config
- Do not insert user-provided sequence names or error strings through `.html()`.
- Treat shared project content as untrusted input.

The current frontend often assembles HTML strings containing names, info, errors, and generated content. Even if the
backend replaces some characters, this is fragile.

### 6. Database And Persistence

Current persistence is minimal, which is good. But it should be made more explicit.

Recommended updates:

- Use Flyway or Liquibase for schema management.
- Add a unique index on shared project `link`.
- Add created/last-access timestamps with `Instant`, not `java.sql.Date`.
- Add expiry/cleanup policy for shared projects.
- Avoid `spring.jpa.hibernate.ddl-auto=update` in production.
- Consider storing compressed JSON if projects can be up to several MB.
- Add a repository method returning `Optional<ProjectEntity>` for unique keys.

Possible entity shape:

```java
@Entity
class SharedProject {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String shareKey;

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    private Instant createdAt;
    private Instant lastAccessedAt;
}
```

### 7. Frontend Should Be Rewritten, Not Incrementally Polished Forever

The frontend is the oldest part of the stack:

- jQuery 1.11
- jQuery UI 1.10
- global variables
- string-built DOM
- inline handlers
- embedded scripts inside Thymeleaf pages
- manual SVG state management
- cross-frame modal plugin pattern

Recommended frontend stack:

- TypeScript
- Vite
- React or Svelte
- SVG rendering kept as first-class code, not string templates
- TanStack Query or a small fetch layer for API calls
- Zustand, Jotai, or Svelte stores for project state
- Playwright for browser-level workflow tests
- Vitest for pure parsing/state logic

React is the conservative choice because the app has complex interactive state, tool panels, dialogs, and SVG views.
Svelte would also fit well if you prefer less boilerplate.

Recommended UI module split:

- `ProjectStore`
- `SequenceList`
- `SequenceEntry`
- `SequenceViewer`
- `AlignmentViewer`
- `Toolbar`
- `SelectionModel`
- `ImportDialog`
- `PrimerDesigner`
- `TranslationViewer`
- `ShareDialog`
- `HelpViewer`

Keep SVG rendering, but isolate it:

- `sequence-layout.ts`
- `sequence-svg-renderer.tsx`
- `alignment-layout.ts`
- `alignment-svg-renderer.tsx`
- `marks.ts`

The rendering logic should take immutable input data and return SVG/React nodes. Selection and drag behavior should live
outside the renderer.

### 8. Consider Moving Some Sequence Logic To The Browser

Many operations are cheap and deterministic:

- reverse
- complement
- reverse complement
- basic validation
- simple translation
- search/find
- extracting subsequences
- marking/coloring
- export to FASTA/HTML

These can run in the browser, ideally in pure TypeScript functions and Web Workers for large inputs. This would reduce
server load and make the UI feel faster.

Keep server-side operations for:

- shared project persistence
- Primer3
- MUSCLE multiple alignment
- WebLogo
- any CPU-heavy or dependency-heavy operation

This would move GeneWarrior closer to a stateless client-heavy tool, matching how the app already works conceptually.

### 9. Bioinformatics Libraries

The custom `genetools` package is a useful local asset, but it should be treated as a domain library with tests, not as
incidental app code.

Options:

1. Keep `genetools`, but modernize it:
    - make classes immutable where possible
    - use records/value objects for positions and marks
    - replace `null` returns with `Optional` or explicit result types
    - replace `System.out.println` with logging
    - add unit tests for translation tables, ambiguity codes, reverse complement, alignments, and edge cases

2. Evaluate BioJava:
    - useful for standard sequence models, parsing, translation, and alignment concepts
    - may reduce maintenance burden
    - may be heavier than needed for this app's focused workflows

Recommended path: keep the local algorithms initially, but wrap them behind clean interfaces so BioJava can be evaluated
operation by operation.

### 10. Testing Strategy

Current tests only check whether the Spring context loads.

Recommended tests:

- Pure unit tests:
    - FASTA import
    - DNA/protein detection
    - invalid character normalization
    - reverse/complement/reverse-complement
    - translation tables
    - ORF detection
    - subsequence extraction with marks
    - mark reverse/translation conversion
    - consensus generation

- API tests:
    - each new endpoint with valid and invalid payloads
    - response status codes
    - payload size limits

- Persistence tests:
    - Testcontainers PostgreSQL
    - share key uniqueness
    - missing key behavior
    - expiry cleanup

- External-tool tests:
    - mock `ExternalToolRunner` for normal tests
    - optional integration profile for installed MUSCLE/Primer3/WebLogo

- Frontend tests:
    - pure state functions with Vitest
    - Playwright workflows for import, select, translate, align, share

### 11. Build, Tooling, And Deployment

Keep the recent Java/Gradle modernization, but add the missing operational pieces.

Recommended updates:

- Add `README` setup for:
    - Java 25
    - Gradle wrapper
    - local H2 profile
    - PostgreSQL profile
    - external tool paths
- Add Dockerfile.
- Add `compose.yaml` for:
    - app
    - PostgreSQL
    - optional external-tool image dependencies
- Add GitHub Actions or similar CI:
    - `./gradlew test`
    - frontend `npm test`
    - Playwright smoke tests
- Add Spotless or Checkstyle for Java formatting.
- Add ESLint and Prettier for the frontend.
- Add dependency vulnerability scanning.

### 12. Observability

Recommended updates:

- Add Spring Boot Actuator.
- Expose health checks:
    - app is alive
    - database is reachable
    - external tools are executable
- Add timing logs or Micrometer metrics for:
    - sequence import
    - alignment
    - primer design
    - WebLogo generation
    - project sharing
- Use structured logging for external tool failures.

## Recommended Target Architecture

```text
frontend/
  Vite + TypeScript + React
  sequence editor
  SVG viewers
  client-side sequence transforms
  API client

backend/
  Spring Boot JSON API
  controllers with typed DTOs
  domain services
  external tool runner
  project sharing persistence

domain/
  sequence models
  translation
  search
  marks
  alignment helpers

infra/
  PostgreSQL
  Flyway migrations
  external tools
  Docker/Compose
```

The backend should not know about frontend DOM concepts. The frontend should not know about backend implementation
classes. They should meet at a documented JSON API.

## Suggested Migration Plan

### Phase 1: Stabilize Current App

- Keep current UI.
- Add unit tests for `DnaToolsService` behavior before splitting it.
- Extract `ExternalToolRunner`.
- Replace `System.out` / `System.err` with SLF4J.
- Add typed configuration properties for website address and tool paths.
- Add basic validation and consistent error handling.
- Add Flyway and a real shared-project migration.

### Phase 2: Create A Clean Backend API

- Add `/api/...` endpoints next to the existing `dnatoolsServlet`.
- Add request/response records.
- Use Jackson instead of direct Gson usage.
- Keep old endpoint temporarily for the legacy UI.
- Build frontend API client against the new endpoints.

### Phase 3: Extract Domain Library

- Move sequence and alignment logic into focused services or a separate module.
- Add thorough tests around biological edge cases.
- Remove HTML construction from backend domain methods.
- Decide whether selected pieces should move to BioJava.

### Phase 4: Rewrite Frontend

- Create `frontend/` with Vite + TypeScript.
- Rebuild the core project workspace first:
    - import
    - list/reorder/delete/select
    - sequence viewer
    - reverse complement
    - translation
    - export
- Rebuild advanced tools:
    - primer designer
    - alignment viewer
    - sequence logo
    - tree viewer
- Use Playwright to compare workflows with the legacy UI.

### Phase 5: Retire Legacy UI

- Remove jQuery, jQuery UI, and `json2.js`.
- Remove inline JavaScript from Thymeleaf templates.
- Serve the frontend as static assets or deploy it separately.
- Keep Spring Boot for API and static hosting if single-deploy simplicity matters.

## Framework Choices

### Backend Recommendation

Use Spring Boot, not a different backend framework.

Why:

- Already in place.
- Good fit for typed JSON APIs, validation, persistence, and operational health checks.
- Good Java 25 support with current Gradle/Spring versions.
- Easy to add Testcontainers, Flyway, Actuator, OpenAPI, and configuration binding.

Add:

- Spring Web MVC
- Bean Validation
- Spring Data JPA
- Flyway
- Testcontainers
- Spring Boot Actuator
- springdoc-openapi

Potentially remove:

- `spring-boot-starter-data-rest`, unless you intentionally expose repository resources. The current app does not appear
  to need repository-driven REST endpoints.
- Gson, once DTOs use Jackson.
- Lombok, if records and normal Java classes are enough.

### Frontend Recommendation

Use Vite + TypeScript + React.

Why:

- Good fit for a complex stateful tool surface.
- Strong ecosystem for SVG-heavy interactive applications.
- TypeScript will catch many mistakes currently hidden in global JavaScript.
- Easy test setup with Vitest and Playwright.

Suggested libraries:

- React
- Vite
- TypeScript
- TanStack Query for server calls
- Zustand or Jotai for project state
- React Hook Form for primer settings forms
- Zod for client-side schema validation
- Playwright for workflow tests

Alternative:

- Svelte + TypeScript would be leaner and pleasant for this UI. Choose it if the team prefers simpler component syntax
  and less ecosystem ceremony.

## Things I Would Not Do First

- Do not start with a full Spring Boot 4 migration. The app is already on a modern enough Spring Boot 3.5 line, and the
  bigger problem is architecture.
- Do not rewrite bioinformatics logic and UI simultaneously. That makes behavior regressions hard to detect.
- Do not replace PostgreSQL unless sharing needs change. The persistence layer is small.
- Do not add authentication unless the product direction requires private projects. Share-by-link can remain anonymous
  if keys are strong and expiry is implemented.
- Do not keep polishing the jQuery UI indefinitely. It is past the point where incremental cleanup will produce a modern
  maintainable frontend.

## Highest-Impact Backlog

1. Add unit tests for sequence import, reverse complement, translation, subsequence extraction, ORF, find, and
   consensus.
2. Extract external process execution into `ExternalToolRunner`.
3. Introduce typed `/api` endpoints beside the legacy endpoint.
4. Replace `outputTypes` with records and Jackson.
5. Add Flyway migration for shared projects.
6. Add security headers and remove raw `.html()` insertion of untrusted values.
7. Start a Vite + TypeScript frontend and rebuild the main sequence workspace.
8. Move simple sequence operations to client-side pure functions or Web Workers.
9. Containerize external tool dependencies.
10. Add Playwright smoke tests for the key workflows.

