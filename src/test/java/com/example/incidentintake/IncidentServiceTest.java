package com.example.incidentintake;

import com.example.incidentintake.audit.AuditService;
import com.example.incidentintake.common.exception.IncidentNotFoundException;
import com.example.incidentintake.common.exception.InvalidStatusTransitionException;
import com.example.incidentintake.incident.api.dto.CreateIncidentRequest;
import com.example.incidentintake.incident.api.dto.IncidentResponse;
import com.example.incidentintake.incident.api.dto.UpdateStatusRequest;
import com.example.incidentintake.incident.application.IncidentService;
import com.example.incidentintake.incident.domain.Incident;
import com.example.incidentintake.incident.domain.IncidentStatus;
import com.example.incidentintake.incident.domain.Severity;
import com.example.incidentintake.incident.infrastructure.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock IncidentRepository incidentRepository;
    @Mock AuditService auditService;

    IncidentService service;

    @BeforeEach
    void setUp() {
        service = new IncidentService(incidentRepository, auditService);
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturnsNewIncident() {
        Incident saved = incident(UUID.randomUUID(), IncidentStatus.OPEN, Severity.HIGH);
        when(incidentRepository.save(any())).thenReturn(saved);

        CreateIncidentRequest req = createRequest("DB down", Severity.HIGH, "alice", null);
        IncidentService.CreateResult result = service.create(req);

        assertThat(result.created()).isTrue();
        assertThat(result.response().getStatus()).isEqualTo(IncidentStatus.OPEN);
        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    void create_idempotency_returnsExistingWhenExternalRefIdMatches() {
        Incident existing = incident(UUID.randomUUID(), IncidentStatus.OPEN, Severity.HIGH);
        when(incidentRepository.findByExternalReferenceId("EXT-1")).thenReturn(Optional.of(existing));

        CreateIncidentRequest req = createRequest("DB down", Severity.HIGH, "alice", "EXT-1");
        IncidentService.CreateResult result = service.create(req);

        assertThat(result.created()).isFalse();
        assertThat(result.response().getId()).isEqualTo(existing.getId());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void create_criticalSeverity_doesNotThrow() {
        Incident saved = incident(UUID.randomUUID(), IncidentStatus.OPEN, Severity.CRITICAL);
        when(incidentRepository.save(any())).thenReturn(saved);

        CreateIncidentRequest req = createRequest("Critical outage", Severity.CRITICAL, "ops", null);
        IncidentService.CreateResult result = service.create(req);

        assertThat(result.created()).isTrue();
    }

    // ── getById ─────────────────────────────────────────────────────────────

    @Test
    void getById_returnsResponseWhenFound() {
        UUID id = UUID.randomUUID();
        Incident inc = incident(id, IncidentStatus.OPEN, Severity.LOW);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(inc));

        IncidentResponse response = service.getById(id);

        assertThat(response.getId()).isEqualTo(id);
    }

    @Test
    void getById_throwsIncidentNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(incidentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(IncidentNotFoundException.class);
    }

    // ── list ────────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void list_withAllNullFilters_callsFindAll() {
        when(incidentRepository.findAll(any(Specification.class))).thenReturn(List.of());

        List<IncidentResponse> results = service.list(null, null, null);

        assertThat(results).isEmpty();
        verify(incidentRepository).findAll(any(Specification.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_withFilters_returnsMatchingIncidents() {
        Incident inc = incident(UUID.randomUUID(), IncidentStatus.OPEN, Severity.HIGH);
        when(incidentRepository.findAll(any(Specification.class))).thenReturn(List.of(inc));

        List<IncidentResponse> results = service.list(Severity.HIGH, IncidentStatus.OPEN, "alice");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSeverity()).isEqualTo(Severity.HIGH);
    }

    // ── updateStatus ─────────────────────────────────────────────────────────

    @Test
    void updateStatus_openToInProgress_succeeds() {
        UUID id = UUID.randomUUID();
        Incident inc = incident(id, IncidentStatus.OPEN, Severity.MEDIUM);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(inc));
        when(incidentRepository.save(inc)).thenReturn(inc);

        UpdateStatusRequest req = updateRequest(IncidentStatus.IN_PROGRESS, "ops", null);
        IncidentResponse response = service.updateStatus(id, req);

        assertThat(response.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        verify(auditService).record(eq(id), eq(IncidentStatus.OPEN), eq(IncidentStatus.IN_PROGRESS), eq("ops"), isNull());
    }

    @Test
    void updateStatus_openToOnHold_succeeds() {
        UUID id = UUID.randomUUID();
        Incident inc = incident(id, IncidentStatus.OPEN, Severity.MEDIUM);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(inc));
        when(incidentRepository.save(inc)).thenReturn(inc);

        UpdateStatusRequest req = updateRequest(IncidentStatus.ON_HOLD, "ops", "waiting for approval");
        IncidentResponse response = service.updateStatus(id, req);

        assertThat(response.getStatus()).isEqualTo(IncidentStatus.ON_HOLD);
        verify(auditService).record(eq(id), eq(IncidentStatus.OPEN), eq(IncidentStatus.ON_HOLD),
                eq("ops"), eq("waiting for approval"));
    }

    @Test
    void updateStatus_inProgressToOnHold_succeeds() {
        UUID id = UUID.randomUUID();
        Incident inc = incident(id, IncidentStatus.IN_PROGRESS, Severity.HIGH);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(inc));
        when(incidentRepository.save(inc)).thenReturn(inc);

        service.updateStatus(id, updateRequest(IncidentStatus.ON_HOLD, "ops", null));

        verify(auditService).record(eq(id), eq(IncidentStatus.IN_PROGRESS), eq(IncidentStatus.ON_HOLD), any(), any());
    }

    @Test
    void updateStatus_onHoldToInProgress_succeeds() {
        UUID id = UUID.randomUUID();
        Incident inc = incident(id, IncidentStatus.ON_HOLD, Severity.HIGH);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(inc));
        when(incidentRepository.save(inc)).thenReturn(inc);

        IncidentResponse response = service.updateStatus(id, updateRequest(IncidentStatus.IN_PROGRESS, "ops", null));

        assertThat(response.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    }

    @Test
    void updateStatus_onHoldToClosed_succeeds() {
        UUID id = UUID.randomUUID();
        Incident inc = incident(id, IncidentStatus.ON_HOLD, Severity.HIGH);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(inc));
        when(incidentRepository.save(inc)).thenReturn(inc);

        IncidentResponse response = service.updateStatus(id, updateRequest(IncidentStatus.CLOSED, "ops", null));

        assertThat(response.getStatus()).isEqualTo(IncidentStatus.CLOSED);
    }

    @Test
    void updateStatus_inProgressToResolved_setsResolvedAt() {
        UUID id = UUID.randomUUID();
        Incident inc = incident(id, IncidentStatus.IN_PROGRESS, Severity.MEDIUM);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(inc));
        when(incidentRepository.save(inc)).thenReturn(inc);

        service.updateStatus(id, updateRequest(IncidentStatus.RESOLVED, "ops", null));

        assertThat(inc.getResolvedAt()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({"RESOLVED", "CLOSED"})
    void updateStatus_openToTerminal_throwsInvalidTransition(String targetStatus) {
        UUID id = UUID.randomUUID();
        Incident inc = incident(id, IncidentStatus.OPEN, Severity.LOW);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(inc));

        UpdateStatusRequest req = updateRequest(IncidentStatus.valueOf(targetStatus), "ops", null);

        assertThatThrownBy(() -> service.updateStatus(id, req))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void updateStatus_closedToAnything_throwsInvalidTransition() {
        UUID id = UUID.randomUUID();
        Incident inc = incident(id, IncidentStatus.CLOSED, Severity.LOW);
        when(incidentRepository.findById(id)).thenReturn(Optional.of(inc));

        assertThatThrownBy(() -> service.updateStatus(id, updateRequest(IncidentStatus.OPEN, "ops", null)))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void updateStatus_incidentNotFound_throwsIncidentNotFound() {
        UUID id = UUID.randomUUID();
        when(incidentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(id, updateRequest(IncidentStatus.IN_PROGRESS, "ops", null)))
                .isInstanceOf(IncidentNotFoundException.class);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private Incident incident(UUID id, IncidentStatus status, Severity severity) {
        Incident inc = Incident.builder()
                .title("Test incident")
                .severity(severity)
                .reportedBy("tester")
                .build();
        inc.setStatus(status);
        // Set id via reflection since @GeneratedValue normally sets it
        try {
            var field = Incident.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(inc, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return inc;
    }

    private CreateIncidentRequest createRequest(String title, Severity severity, String reportedBy, String extRef) {
        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setTitle(title);
        req.setSeverity(severity);
        req.setReportedBy(reportedBy);
        req.setExternalReferenceId(extRef);
        return req;
    }

    private UpdateStatusRequest updateRequest(IncidentStatus status, String changedBy, String notes) {
        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus(status);
        req.setChangedBy(changedBy);
        req.setNotes(notes);
        return req;
    }
}
