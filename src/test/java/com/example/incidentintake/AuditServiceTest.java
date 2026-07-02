package com.example.incidentintake;

import com.example.incidentintake.audit.AuditLogRepository;
import com.example.incidentintake.audit.AuditLogResponse;
import com.example.incidentintake.audit.AuditService;
import com.example.incidentintake.audit.IncidentAuditLog;
import com.example.incidentintake.incident.domain.IncidentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock AuditLogRepository auditLogRepository;

    AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository);
    }

    @Test
    void record_savesAuditLogWithCorrectFields() {
        String incidentId = "INC1001";

        auditService.record(incidentId, IncidentStatus.OPEN, IncidentStatus.IN_PROGRESS, "alice", "taking it");

        ArgumentCaptor<IncidentAuditLog> captor = ArgumentCaptor.forClass(IncidentAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        IncidentAuditLog saved = captor.getValue();
        assertThat(saved.getIncidentId()).isEqualTo(incidentId);
        assertThat(saved.getPreviousStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(saved.getNewStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(saved.getChangedBy()).isEqualTo("alice");
        assertThat(saved.getNotes()).isEqualTo("taking it");
        assertThat(saved.getChangedAt()).isNotNull();
    }

    @Test
    void record_withNullNotes_savesWithNullNotes() {
        String incidentId = "INC1002";

        auditService.record(incidentId, IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED, "bob", null);

        ArgumentCaptor<IncidentAuditLog> captor = ArgumentCaptor.forClass(IncidentAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getNotes()).isNull();
    }

    @Test
    void getHistory_returnsEmptyListWhenNoLogs() {
        String incidentId = "INC1003";
        when(auditLogRepository.findByIncidentIdOrderByChangedAtAsc(incidentId)).thenReturn(List.of());

        List<AuditLogResponse> history = auditService.getHistory(incidentId);

        assertThat(history).isEmpty();
    }

    @Test
    void getHistory_returnsMappedResponsesInOrder() {
        String incidentId = "INC1004";
        Instant t1 = Instant.now().minusSeconds(60);
        Instant t2 = Instant.now();

        IncidentAuditLog log1 = IncidentAuditLog.builder()
                .id(UUID.randomUUID()).incidentId(incidentId)
                .previousStatus(IncidentStatus.OPEN).newStatus(IncidentStatus.IN_PROGRESS)
                .changedBy("alice").changedAt(t1).notes("started").build();
        IncidentAuditLog log2 = IncidentAuditLog.builder()
                .id(UUID.randomUUID()).incidentId(incidentId)
                .previousStatus(IncidentStatus.IN_PROGRESS).newStatus(IncidentStatus.RESOLVED)
                .changedBy("bob").changedAt(t2).notes(null).build();

        when(auditLogRepository.findByIncidentIdOrderByChangedAtAsc(incidentId))
                .thenReturn(List.of(log1, log2));

        List<AuditLogResponse> history = auditService.getHistory(incidentId);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getPreviousStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(history.get(0).getNewStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(history.get(0).getNotes()).isEqualTo("started");
        assertThat(history.get(1).getNewStatus()).isEqualTo(IncidentStatus.RESOLVED);
    }
}
