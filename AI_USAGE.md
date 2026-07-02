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

## Key Prompts Used [exact prompts available in prompts.md]

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

---

## Innovation — AI Features [Chat interface requires an anthropic API key to be added]

This service is built AI-native from the ground up: every capability is available to both humans (REST) and AI agents (MCP, natural-language chat) through the same underlying business logic, with no divergent code paths.

| Feature | What it does | Where |
|---|---|---|
| **MCP tool exposure** | All 5 core operations (create, get, list, update status, get history) are registered as [Model Context Protocol](https://modelcontextprotocol.io/) tools with full JSON Schema, so any MCP-aware AI agent (Claude Desktop, IDE assistants, custom agents) can operate the service directly — no custom integration code required. | [`IncidentMcpTools`](src/main/java/com/example/incidentintake/mcp/IncidentMcpTools.java), [MCP Connection](#mcp-connection) |
| **Natural-language chat interface** | `POST /chat` puts a Claude model (via Spring AI) in front of the same MCP tools, so operators can manage incidents conversationally — *"Create a CRITICAL incident for the Redis cluster failover, then show me its history"* — without knowing the REST API shape at all. | [`ChatController`](src/main/java/com/example/incidentintake/chat/ChatController.java), [Chat Interface](#chat-interface-claude) |
| **Multi-turn conversation memory** | Chat requests carry a `conversationId`; `MessageChatMemoryAdvisor` threads prior turns back into each prompt, so the model resolves references like *"that incident"* or *"the one I just created"* without the caller repeating IDs. | [`ChatController`](src/main/java/com/example/incidentintake/chat/ChatController.java) |
| **Self-describing tool discovery** | `GET /mcp/tools` introspects the registered `ToolCallbackProvider` and returns every tool's name, description, and input schema — a single discovery endpoint an agent (or a human debugging one) can hit to learn the full capability surface without reading source code. | [`McpContextController`](src/main/java/com/example/incidentintake/mcp/McpContextController.java) |
| **Idempotent agent-safe writes** | `create_incident`/`POST /incidents` accepts an optional `externalReferenceId`; resubmitting the same value returns the existing record (`200`) instead of a duplicate (`201`). This matters specifically for AI agents, which may retry a tool call after an ambiguous or truncated response. | [`IncidentService`](src/main/java/com/example/incidentintake/incident/application/IncidentService.java) |
| **Fail-fast AI configuration errors** | If `ANTHROPIC_API_KEY` isn't set, `/chat` returns a clear `503` immediately instead of a cryptic upstream failure — a small but deliberate choice to keep AI-feature failures diagnosable rather than mysterious. | [`ChatController`](src/main/java/com/example/incidentintake/chat/ChatController.java) |

