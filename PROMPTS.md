# Prompt History

A structured log of the prompts given to the AI assistant (Claude Code) across chat sessions while building and evolving this service.

---

## Session 1 — Initial Build (verbatim)

The full specification prompt that kicked off the project. It was given twice, identically, across two separate chat windows for the initial build.

```text
You are an expert Spring Boot engineer. Build a production-ready incident intake
microservice using Spring Boot 3.5+ and Java 21. Follow these requirements precisely.

## Build Tool and Project Setup
- Use Maven, not Gradle
- Generate the project using Spring Initializr
- Dependencies:
  - Spring Web
  - Spring Data JPA
  - Validation
  - Actuator
  - Lombok
  - H2 Database
  - PostgreSQL Driver
  - Spring AI MCP Server starter
  - Spring Boot Test
- Use a pom.xml with clean dependency management
- Use Spring AI BOM in Maven dependencyManagement
- Use standard Maven folder structure:
  - src/main/java
  - src/main/resources
  - src/test/java
- Java version: 21
- Packaging: jar

## pom.xml expectations
- Parent: spring-boot-starter-parent
- Include Spring AI BOM under <dependencyManagement>
- Use Maven Central only unless a milestone or snapshot dependency is truly required
- Include plugins:
  - spring-boot-maven-plugin
  - maven-compiler-plugin configured for Java 21
  - surefire plugin if needed for test setup

## Domain: Incident Lifecycle
An Incident has:
- id (UUID, internal, auto-generated)
- title (String, required)
- severity (Enum: LOW | MEDIUM | HIGH | CRITICAL, required)
- reportedBy (String, required)
- externalReferenceId (String, optional)
- status (Enum: OPEN | IN_PROGRESS | RESOLVED | CLOSED)
- createdAt (timestamp)
- updatedAt (timestamp)
- resolvedAt (nullable timestamp)

An IncidentAuditLog has:
- id (UUID)
- incidentId (UUID FK)
- previousStatus
- newStatus
- changedBy
- changedAt
- notes (optional)

This audit model must answer:
"What happened to this incident, and when?"

## API Endpoints

### POST /incidents
- Required fields: title, severity, reportedBy
- Optional field: externalReferenceId
- If externalReferenceId is supplied and the same value is submitted twice,
  handle it gracefully by returning HTTP 200 with the existing incident.
- Document this idempotency decision clearly in code comments and README.
- Return an IncidentResponse DTO

### GET /incidents/{id}
- Retrieve one incident by internal UUID
- Return 404 with structured error if not found

### GET /incidents
- Support filtering by at least:
  - severity
  - status
  - reportedBy
- Filters should be optional and combinable

### PATCH /incidents/{id}/status
- Request body includes:
  - status
  - changedBy
  - notes (optional)
- Enforce allowed transitions:
  - OPEN -> IN_PROGRESS
  - IN_PROGRESS -> RESOLVED
  - RESOLVED -> CLOSED
- Reject invalid transitions with HTTP 422
- Persist an IncidentAuditLog row for every valid change

### GET /incidents/{id}/history
- Return audit trail ordered by changedAt ascending

## Error Handling
- Implement a global @ControllerAdvice
- Return one consistent machine-readable error format for all failures:
  {
    "timestamp": "...",
    "status": 400,
    "error": "Bad Request",
    "message": "...",
    "path": "/incidents",
    "fieldErrors": [...]
  }
- Cover validation errors, not found errors, invalid state transitions, and unexpected exceptions

## Structured Logging
- Use structured JSON logging suitable for downstream parsing
- Include requestId, method, path, durationMs
- Log business events such as:
  - INCIDENT_CREATED
  - INCIDENT_STATUS_CHANGED
- For CRITICAL incidents, log at WARN level
- Use MDC for correlation IDs and incidentId where useful

## MCP Tool Exposure
Expose the service capabilities as MCP tools so AI agents can use them directly.

- Add Spring AI MCP Server dependency via Maven
- Create a dedicated MCP tool adapter/service
- Expose tools such as:
  - create_incident
  - get_incident
  - list_incidents
  - update_incident_status
  - get_incident_history
- Each tool must have a clear name, description, and strongly typed input
- Configure MCP transport over HTTP/SSE or the currently recommended Spring AI web transport
- Add README instructions showing how an MCP-compatible client or agent can connect

Important:
Design the MCP layer as a thin adapter over the application service layer.
Do not place core business logic inside MCP tool methods.


Do not over-engineer this part.
Keep the architecture clean so an AI client or agent can be added safely later.

## Agentic AI implementation expectations
Build this using an agentic workflow, not simple autocomplete.

Expected workflow:
1. Plan the package structure
2. Create pom.xml
3. Scaffold domain, DTOs, repositories, services, controllers
4. Add global exception handling
5. Add MCP tools
6. Add structured logging
7. Add tests
8. Update README.md
9. Create AI_USAGE.md
10. Run mvn test and fix failures

The agent should work iteratively and keep the code compiling after each major step.

## Package Structure
Use a clean, production-style structure.
Prefer feature-oriented packaging or a clean layered structure under a root package.

Example:
- com.example.incidentintake
  - incident
    - api
    - application
    - domain
    - infrastructure
  - audit
  - mcp
  - common
  - config

Keep the @SpringBootApplication class in the root package.

## Testing
Focus tests on the highest-risk behavior:
- POST /incidents validation
- externalReferenceId idempotency behavior
- GET by id not found case
- filter behavior for GET /incidents
- valid and invalid status transitions
- audit history correctness
- MCP tool wiring where practical
- integration tests using Spring Boot Test and MockMvc or WebTestClient

The build must pass with:
- mvn test

## README.md
Include:
- Prerequisites
- How to run with Maven:
  - mvn spring-boot:run
- How to test:
  - mvn test
- How to package:
  - mvn clean package
- How to run with Postgres profile
- Example curl commands for every endpoint
- MCP connection instructions
- Idempotency behavior explanation for externalReferenceId

## AI_USAGE.md
Create AI_USAGE.md documenting:
- Which AI tools were used
- Where agentic AI was used
- Which files or modules were primarily AI-generated
- What human review or validation was performed
- What prompts were used

## Non-functional expectations
- Thin controllers, business logic in services
- DTOs separated from entities
- Validation annotations on request DTOs
- Auditable lifecycle design
- Clean exception model
- Readable code
- Comments only where they add value
- No unfinished TODOs
- Final result should compile, tests should pass, and documentation should be complete
```

---

## Session 2 — Current Chat Session (verbatim)

| # | Prompt | Outcome |
|---|---|---|
| 1 | "check what kind of tests are present in this repo unit or integration as well" | Surveyed `src/test/java` and classified all test files as integration (`@SpringBootTest`), unit (Mockito), or plain JUnit. |
| 2 | "add this to a test plan md file in this repo\nalso run all tests and show coverage" | Created `TEST_PLAN.md`; added JaCoCo to `pom.xml`; ran the full suite and reported coverage numbers. |
| 3 | "make sure previous change are commited..\nhow do we handle assigned to flow who is it assigned to\nalso can we add a basic authentication" | Committed pending changes; investigated (and reported the absence of) an `assignedTo`/assignment flow; added Spring Security HTTP Basic auth across all endpoints. |
| 4 | "update with tests, update postman collection with basoc auth detais, update arch and readme as well\nno go on the assignedTo feature" | Added `SecurityConfigTest`; added collection-level Basic auth to the Postman collection; updated `architecture.html` with a Security layer; updated `README.md` with an Authentication section. Explicitly skipped the `assignedTo` feature per instruction. |
| 5 | "Verify basic auth works: curl the running app's /incidents endpoint without and with credentials." | Started the app, curled `/incidents` with no creds (401), wrong creds (401), and correct creds (200), confirmed `/actuator/health` stays public, then stopped the app. |
| 6 | "test everything with curl" | Ran a full curl regression across auth, CRUD, idempotency, all status transitions (including `ON_HOLD`), invalid-transition 422, audit history, MCP tools list, validation errors, and actuator endpoints. |
| 7 | "in readme md include innovation section and put ai features" | Added an "Innovation — AI Features" section to `README.md` plus a new "Chat Interface (Claude)" section documenting the previously-undocumented `POST /chat` endpoint. |
| 8 | "add a file prompts and add all the prompts from start in a tructured format that were given across chat windows" | This file. |
| 9 | "push evereything" | Committed a few pending manual edits found in the working tree, then attempted `git push`; blocked on an interactive GitHub Credential Manager login that can't be completed from this environment — surfaced to the user rather than hanging or force-bypassing auth. |
| 10 | *(interrupt)* "include this in prompts" + the full Session 1 specification prompt, pasted twice verbatim | Replaced the summarized Session 1 entry above with the actual verbatim specification text. |

---

## Recurring Operational Notes

Patterns worth carrying forward from this session:

- **JDK mismatch:** the machine only has JDK 17 installed, but `pom.xml` targets Java 21 (matching IntelliJ's project SDK, which isn't actually present on disk). Every `mvn`/`spring-boot:run` invocation in this session required temporarily dropping `java.version`/`source`/`target` to 17, running, then restoring to 21 before committing — always verify `git diff pom.xml` is clean afterward.
- **Maven location:** not on `PATH`; invoked directly via `C:\Users\kittu\apache-maven\apache-maven-3.9.6\bin\mvn.cmd`.
- **Background server verification:** starting the app with `run_in_background` and polling `netstat` for port 8080 is the reliable pattern; `Start-Job` does not persist across separate PowerShell tool calls in this environment.
