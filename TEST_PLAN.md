# Test Plan — Incident Intake Service

## Test Suite Overview

The project contains **8 test files** split across three categories.

---

## Integration Tests

> Use `@SpringBootTest` — load the full Spring application context (in-memory H2 database).

| File | What it tests |
|---|---|
| `AuditHistoryTest.java` | End-to-end audit log recording via the real application context |
| `IncidentControllerTest.java` | HTTP layer + service + repository wired together |
| `StatusTransitionTest.java` | State machine transitions through the full stack |

---

## Unit Tests (Mockito)

> Use `@ExtendWith(MockitoExtension.class)` — dependencies are mocked with Mockito.

| File | What it tests |
|---|---|
| `AuditServiceTest.java` | `AuditService` in isolation, mocking `AuditLogRepository` |
| `IncidentServiceTest.java` | `IncidentService` in isolation, mocking `IncidentRepository` + `AuditService` |
| `IncidentMcpToolsTest.java` | MCP tool layer in isolation, mocking `IncidentService` + `AuditService` |

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
| Integration tests (`@SpringBootTest`) | 3 |
| Unit tests (Mockito) | 3 |
| Unit tests (Plain JUnit) | 2 |
| **Total test files** | **8** |

---

## Test Run Results

**Last run:** 2026-07-02

```
Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS — Total time: 30.911 s
```

---

## Code Coverage (JaCoCo)

**Overall: 97% instruction coverage, 91% branch coverage** across 16 classes

| Package | Instruction Cov. | Branch Cov. | Lines | Methods | Classes |
|---|---|---|---|---|---|
| `audit` | **100%** | n/a | 28 | 5 | 3 |
| `mcp` | **100%** | **100%** | 22 | 6 | 1 |
| `incident.domain` | **100%** | n/a | 4 | 2 | 2 |
| `incident.api.dto` | **100%** | n/a | 11 | 1 | 1 |
| `config` | **100%** | n/a | 2 | 2 | 1 |
| `common.exception` | 99% | 50% | 28 | 12 | 3 |
| `incident.application` | 98% | 94% | 67 | 15 | 2 |
| `common.logging` | 93% | 50% | 17 | 3 | 1 |
| `incident.api` | 86% | **100%** | 6 | 4 | 1 |
| `(root — main class)` | 37% | n/a | 3 | 2 | 1 |

> The root package's 37% reflects the Spring Boot main class (`main` method), which is not exercised by unit/integration tests — this is expected and not a coverage concern.
