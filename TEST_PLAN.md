# Test Plan — Incident Intake Service

## Test Suite Overview

The project contains **14 test files** split across three categories.

---

## Integration Tests

> Use `@SpringBootTest` — load the full Spring application context (in-memory H2 database).

| File | What it tests |
|---|---|
| `AuditHistoryTest.java` | End-to-end audit log recording via the real application context |
| `IncidentControllerTest.java` | HTTP layer + service + repository wired together |
| `StatusTransitionTest.java` | State machine transitions through the full stack |
| `ChatEndpointIntegrationTest.java` | POST /chat fails fast with a clear error when the Anthropic API key isn't configured |
| `McpToolsEndpointIntegrationTest.java` | GET /mcp/tools returns all registered MCP tools with schemas |
| `SecurityConfigTest.java` | HTTP Basic auth enforcement — 401 without/wrong credentials, 200 with valid credentials, public access to `/actuator/health` |

---

## Unit Tests (Mockito)

> Use `@ExtendWith(MockitoExtension.class)` — dependencies are mocked with Mockito.

| File | What it tests |
|---|---|
| `AuditServiceTest.java` | `AuditService` in isolation, mocking `AuditLogRepository` |
| `IncidentServiceTest.java` | `IncidentService` in isolation, mocking `IncidentRepository` + `AuditService` |
| `IncidentMcpToolsTest.java` | MCP tool layer in isolation, mocking `IncidentService` + `AuditService` |
| `ChatControllerTest.java` | `ChatController` in isolation, mocking `ChatClient` + `ChatMemory` |
| `IncidentIdGeneratorTest.java` | Human-readable `INC<number>` ID generation, mocking `IncidentRepository` |
| `McpContextControllerTest.java` | MCP tool introspection endpoint, mocking `ToolCallbackProvider` |

---

## Unit Tests (Plain JUnit)

> No Spring context or Mockito — pure logic tests with minimal stubs.

| File | What it tests |
|---|---|
| `GlobalExceptionHandlerTest.java` | Exception handler behavior with a minimal stub controller |
| `RequestLoggingFilterTest.java` | Logging filter logic directly |

---

## Summary

| Category | Count |
|---|---|
| Integration tests (`@SpringBootTest`) | 6 |
| Unit tests (Mockito) | 6 |
| Unit tests (Plain JUnit) | 2 |
| **Total test files** | **14** |

---

## Test Run Results

**Last run:** 2026-07-02

```
Tests run: 89, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Code Coverage (JaCoCo)

**Overall: 98% instruction coverage (1,316/1,336), 92% branch coverage (47/51)** across 25 classes.

Run `mvn test` to regenerate the report at `target/site/jacoco/index.html`.

---

## Authentication in Tests

All `@SpringBootTest` classes that exercise the real HTTP stack via `MockMvc` (`AuditHistoryTest`, `ChatEndpointIntegrationTest`, `IncidentControllerTest`, `McpToolsEndpointIntegrationTest`, `StatusTransitionTest`) are annotated with `@WithMockUser` so requests pass Spring Security's authentication filter without needing real Basic auth headers on every call. `SecurityConfigTest` is the exception — it deliberately omits `@WithMockUser` to verify the real HTTP Basic auth flow (401 without/wrong credentials, 200 with valid ones).
