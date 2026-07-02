# Prompt History

A structured log of the prompts given to the AI assistant (Claude Code) across chat sessions while building and evolving this service.

> **Note on scope:** Only the current chat session's prompts are recorded here verbatim — that's the extent of what's directly accessible from within a session. The initial build (before this session) is summarized in [AI_USAGE.md](AI_USAGE.md#key-prompts-used) rather than reproduced verbatim, since only a paraphrase of that original specification prompt was retained.

---

## Session 1 — Initial Build (summarized, not verbatim)

See [AI_USAGE.md](AI_USAGE.md) for full detail. In brief, a single detailed specification prompt covered:
- Build tool, Spring Boot version, Java version, packaging
- Full domain model (`Incident` + `IncidentAuditLog`)
- REST API contract (5 endpoints with exact semantics)
- Status transition rules
- Idempotency requirement for `externalReferenceId`
- Error response format
- Structured logging requirements
- MCP tool exposure requirements
- Test coverage expectations
- Documentation requirements

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

---

## Recurring Operational Notes

Patterns worth carrying forward from this session:

- **JDK mismatch:** the machine only has JDK 17 installed, but `pom.xml` targets Java 21 (matching IntelliJ's project SDK, which isn't actually present on disk). Every `mvn`/`spring-boot:run` invocation in this session required temporarily dropping `java.version`/`source`/`target` to 17, running, then restoring to 21 before committing — always verify `git diff pom.xml` is clean afterward.
- **Maven location:** not on `PATH`; invoked directly via `C:\Users\kittu\apache-maven\apache-maven-3.9.6\bin\mvn.cmd`.
- **Background server verification:** starting the app with `run_in_background` and polling `netstat` for port 8080 is the reliable pattern; `Start-Job` does not persist across separate PowerShell tool calls in this environment.
