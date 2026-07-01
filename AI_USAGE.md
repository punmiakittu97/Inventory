# AI Usage Documentation

## Overview

This microservice was built using an agentic AI workflow powered by **Claude Sonnet 4.6** via Claude Code (the Anthropic CLI tool). The AI acted as a senior Spring Boot engineer, planning the architecture, scaffolding the full codebase, and iterating to fix compilation and test failures.

---

## AI Tools Used

| Tool | Purpose |
|---|---|
| Claude Code (Claude Sonnet 4.6) | End-to-end code generation, architecture planning, test authoring, documentation |
| Claude Code Plan Mode | Structured multi-phase planning before any file was written |

---

## Where Agentic AI Was Used

The AI followed an explicit agentic workflow rather than simple autocomplete:

1. **Planning phase** — Analyzed requirements, designed the package structure, chose patterns (JpaSpecificationExecutor for filtering, record type for CreateResult, switch expression for transitions), and wrote the full plan to a markdown file before touching any code.
2. **Scaffolding** — Created all Maven directories, `pom.xml`, enums, entities, repositories, DTOs, services, controllers, exception handlers, and filters in structured dependency order to keep the project compiling after each step.
3. **MCP layer** — Designed the thin adapter pattern: `IncidentMcpTools` delegates directly to `IncidentService` / `AuditService` with no duplicated business logic.
4. **Test authoring** — Wrote integration tests covering all high-risk paths: idempotency, valid/invalid transitions, audit ordering, and MCP tool delegation.
5. **Documentation** — Generated `README.md` with curl examples for every endpoint and MCP connection instructions, plus this file.

---

## Primarily AI-Generated Files

All files in this project were primarily AI-generated:

- `pom.xml` — dependency selection, BOM management, plugin configuration
- All `src/main/java/**` — domain model, service layer, controllers, MCP tools, exception handling, logging filter
- `src/main/resources/` — `application.properties`, `application-postgres.properties`, `logback-spring.xml`
- `src/test/java/**` — four test classes covering the full test plan
- `README.md` — all documentation including curl examples and MCP instructions
- `architecture.html` — interactive architecture diagram

---

## Human Review / Validation

- Requirements were provided by a human engineer via a detailed specification prompt.
- The human reviewed the plan in Plan Mode before approving execution.
- Final build validation (`mvn test`) confirms correctness of generated code.
- Architectural decisions (thin MCP adapter, idempotency via externalReferenceId, JpaSpecificationExecutor for filtering) were validated against the specification before implementation.

---

## Key Prompts Used

The primary prompt was a detailed specification covering:
- Build tool, Spring Boot version, Java version, packaging
- Full domain model (Incident + IncidentAuditLog)
- REST API contract (5 endpoints with exact semantics)
- Status transition rules
- Idempotency requirement for externalReferenceId
- Error response format
- Structured logging requirements
- MCP tool exposure requirements
- Test coverage expectations
- Documentation requirements

The AI was instructed to follow an agentic workflow: plan → scaffold → implement → test → document → verify.
