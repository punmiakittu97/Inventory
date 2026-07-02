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
curl -s -X POST http://localhost:8080/incidents \
  -H "Content-Type: application/json" \
  -d '{"title":"Database down","severity":"CRITICAL","reportedBy":"alice","externalReferenceId":"EXT-001"}' | jq .

# Idempotent re-submit (200, same ID returned)
curl -s -X POST http://localhost:8080/incidents \
  -H "Content-Type: application/json" \
  -d '{"title":"Database down","severity":"CRITICAL","reportedBy":"alice","externalReferenceId":"EXT-001"}' | jq .
```

### GET /incidents/{id} — Get a single incident

```bash
curl -s http://localhost:8080/incidents/<id> | jq .
```

Returns `404` with a structured error body if not found.

### GET /incidents — List incidents (with optional filters)

All query parameters are optional and combinable:

```bash
# All incidents
curl -s http://localhost:8080/incidents | jq .

# Filter by severity
curl -s "http://localhost:8080/incidents?severity=CRITICAL" | jq .

# Filter by status
curl -s "http://localhost:8080/incidents?status=OPEN" | jq .

# Filter by reporter
curl -s "http://localhost:8080/incidents?reportedBy=alice" | jq .

# Combined filter
curl -s "http://localhost:8080/incidents?severity=HIGH&status=IN_PROGRESS" | jq .
```

### PATCH /incidents/{id}/status — Update status

Allowed transitions: `OPEN → IN_PROGRESS → RESOLVED → CLOSED`

```bash
# Move to IN_PROGRESS
curl -s -X PATCH http://localhost:8080/incidents/<id>/status \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS","changedBy":"ops-team","notes":"Investigating"}' | jq .

# Resolve
curl -s -X PATCH http://localhost:8080/incidents/<id>/status \
  -H "Content-Type: application/json" \
  -d '{"status":"RESOLVED","changedBy":"ops-team","notes":"Root cause: disk full. Fixed."}' | jq .

# Close
curl -s -X PATCH http://localhost:8080/incidents/<id>/status \
  -H "Content-Type: application/json" \
  -d '{"status":"CLOSED","changedBy":"manager"}' | jq .

# Invalid transition (returns 422)
curl -s -X PATCH http://localhost:8080/incidents/<id>/status \
  -H "Content-Type: application/json" \
  -d '{"status":"CLOSED","changedBy":"ops"}' | jq .
```

### GET /incidents/{id}/history — Audit trail

```bash
curl -s http://localhost:8080/incidents/<id>/history | jq .
```

---

## MCP Connection

The service exposes all capabilities as MCP tools over HTTP/SSE.

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
      "url": "http://localhost:8080/sse"
    }
  }
}
```

Or use the Spring AI MCP client:

```java
var transport = new HttpSseClientTransport("http://localhost:8080/sse");
var client = McpClient.sync(transport).build();
client.initialize();
// call tools...
```

---

## Actuator Health

```bash
curl -s http://localhost:8080/actuator/health | jq .
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
