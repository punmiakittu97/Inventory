# Incident Intake Microservice

A production-ready incident lifecycle management service built with Spring Boot 3.5+ and Java 21. Exposes a REST API, enforces status transitions with an audit trail, and wraps all capabilities as MCP tools for AI agent consumption.

---


## Prerequisites

- Java 21+
- Maven 3.9+
- (Optional) PostgreSQL 15+ for production profile

---

## Running the Service

### With H2 (default — no database setup required)

```bash
mvn spring-boot:run
```

Service starts on `http://localhost:8080`.

### With PostgreSQL

Ensure a PostgreSQL instance is running with a database named `incidentdb`:

```sql
CREATE DATABASE incidentdb;
```

Then run:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

Default credentials in `application-postgres.properties`: `postgres / postgres`. Override via environment variables:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://myhost:5432/incidentdb \
SPRING_DATASOURCE_USERNAME=myuser \
SPRING_DATASOURCE_PASSWORD=mypass \
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

---

## Authentication

All endpoints require HTTP Basic auth, except `/actuator/health` and `/h2-console`.

Default credentials (local dev): `admin` / `changeme`. Override via environment variables:

```bash
APP_SECURITY_USERNAME=myuser \
APP_SECURITY_PASSWORD=mypass \
mvn spring-boot:run
```

Include credentials on every request:

```bash
curl -s -u admin:changeme http://localhost:8080/incidents | jq .
```

Requests without valid credentials return `401 Unauthorized`.

---

## Running Tests

```bash
mvn test
```

All tests run against an in-memory H2 database — no external services needed.

---

## Packaging

```bash
mvn clean package
java -jar target/incident-intake-0.0.1-SNAPSHOT.jar
```

---

## API Reference

### POST /incidents — Create an incident

> **Idempotency:** If `externalReferenceId` is supplied and an incident with that value already exists,
> the service returns `HTTP 200` with the existing incident rather than creating a duplicate.
> A fresh creation returns `HTTP 201`. This allows upstream systems to safely retry without producing
> duplicate records.

```bash
# Create (201)
curl -s -u admin:changeme -X POST http://localhost:8080/incidents \
  -H "Content-Type: application/json" \
  -d '{"title":"Database down","severity":"CRITICAL","reportedBy":"alice","externalReferenceId":"EXT-001"}' | jq .

# Idempotent re-submit (200, same ID returned)
curl -s -u admin:changeme -X POST http://localhost:8080/incidents \
  -H "Content-Type: application/json" \
  -d '{"title":"Database down","severity":"CRITICAL","reportedBy":"alice","externalReferenceId":"EXT-001"}' | jq .
```

### GET /incidents/{id} — Get a single incident

```bash
curl -s -u admin:changeme http://localhost:8080/incidents/<id> | jq .
```

Returns `404` with a structured error body if not found.

### GET /incidents — List incidents (with optional filters)

All query parameters are optional and combinable:

```bash
# All incidents
curl -s -u admin:changeme http://localhost:8080/incidents | jq .

# Filter by severity
curl -s -u admin:changeme "http://localhost:8080/incidents?severity=CRITICAL" | jq .

# Filter by status
curl -s -u admin:changeme "http://localhost:8080/incidents?status=OPEN" | jq .

# Filter by reporter
curl -s -u admin:changeme "http://localhost:8080/incidents?reportedBy=alice" | jq .

# Combined filter
curl -s -u admin:changeme "http://localhost:8080/incidents?severity=HIGH&status=IN_PROGRESS" | jq .
```

### PATCH /incidents/{id}/status — Update status

Allowed transitions: `OPEN → IN_PROGRESS → RESOLVED → CLOSED`

```bash
# Move to IN_PROGRESS
curl -s -u admin:changeme -X PATCH http://localhost:8080/incidents/<id>/status \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS","changedBy":"ops-team","notes":"Investigating"}' | jq .

# Resolve
curl -s -u admin:changeme -X PATCH http://localhost:8080/incidents/<id>/status \
  -H "Content-Type: application/json" \
  -d '{"status":"RESOLVED","changedBy":"ops-team","notes":"Root cause: disk full. Fixed."}' | jq .

# Close
curl -s -u admin:changeme -X PATCH http://localhost:8080/incidents/<id>/status \
  -H "Content-Type: application/json" \
  -d '{"status":"CLOSED","changedBy":"manager"}' | jq .

# Invalid transition (returns 422)
curl -s -u admin:changeme -X PATCH http://localhost:8080/incidents/<id>/status \
  -H "Content-Type: application/json" \
  -d '{"status":"CLOSED","changedBy":"ops"}' | jq .
```

### GET /incidents/{id}/history — Audit trail

```bash
curl -s -u admin:changeme http://localhost:8080/incidents/<id>/history | jq .
```

---

## Chat Interface (Claude)

`POST /chat` puts a Claude model in front of all five MCP tools, so incidents can be created, queried, and transitioned through natural language instead of structured API calls. Requires `ANTHROPIC_API_KEY` to be set — without it, the endpoint returns a clear `503` rather than an obscure upstream failure.

```bash
# First turn — no conversationId, server mints one
curl -s -u admin:changeme -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Create a CRITICAL incident titled '\''Redis cluster failover loop'\'', reported by ops@example.com."}' | jq .

# Follow-up turn — reuse conversationId from the previous response for multi-turn memory
curl -s -u admin:changeme -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Move that incident to IN_PROGRESS, changedBy is ops@example.com.","conversationId":"<conversationId from previous response>"}' | jq .
```

Response shape:

```json
{
  "reply": "I've created incident INC1003 with CRITICAL severity...",
  "conversationId": "3b469527-7b73-4e85-b2a3-227ddb2ce286"
}
```

The model resolves references like "that incident" using `conversationId`-scoped memory (`MessageChatMemoryAdvisor`) — no need to repeat incident IDs across turns.

---

## MCP Connection

The service exposes all capabilities as MCP tools over HTTP/SSE. Like the REST API, MCP endpoints require HTTP Basic auth.

**SSE endpoint:** `http://localhost:8080/sse`  
**Message endpoint:** `http://localhost:8080/mcp/messages`

### Available tools

| Tool name | Description |
|---|---|
| `create_incident` | Create a new incident (idempotent via externalReferenceId) |
| `get_incident` | Retrieve an incident by ID (e.g. INC1001) |
| `list_incidents` | List incidents with optional severity/status/reportedBy filters |
| `update_incident_status` | Transition incident status with audit trail |
| `get_incident_history` | Retrieve full audit history ordered by time |

### Connecting an MCP client

Add the following to your MCP client configuration (e.g. Claude Desktop `claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "incident-intake": {
      "url": "http://localhost:8080/sse",
      "headers": {
        "Authorization": "Basic YWRtaW46Y2hhbmdlbWU="
      }
    }
  }
}
```

> The `Authorization` header value above is `admin:changeme` base64-encoded. Generate your own with `echo -n 'user:pass' | base64`.

Or use the Spring AI MCP client:

```java
var transport = HttpSseClientTransport.builder("http://localhost:8080")
        .sseEndpoint("/sse")
        .customizeClient(builder -> builder.defaultHeaders(h -> h.setBasicAuth("admin", "changeme")))
        .build();
var client = McpClient.sync(transport).build();
client.initialize();
// call tools...
```

---

## Actuator Health

`/actuator/health` is publicly accessible (no auth required):

```bash
curl -s http://localhost:8080/actuator/health | jq .
```

Other actuator endpoints (`/actuator/info`, `/actuator/metrics`) require Basic auth like the rest of the API:

```bash
curl -s -u admin:changeme http://localhost:8080/actuator/metrics | jq .
```

---

## Error Format

All errors return a consistent JSON body:

```json
{
  "timestamp": "2026-07-01T10:00:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Invalid status transition from OPEN to CLOSED",
  "path": "/incidents/abc/status",
  "fieldErrors": []
}
```
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

---