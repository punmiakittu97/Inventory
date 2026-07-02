package com.example.incidentintake;

import com.example.incidentintake.audit.AuditService;
import com.example.incidentintake.incident.api.dto.CreateIncidentRequest;
import com.example.incidentintake.incident.api.dto.IncidentResponse;
import com.example.incidentintake.incident.api.dto.UpdateStatusRequest;
import com.example.incidentintake.incident.application.IncidentService;
import com.example.incidentintake.incident.domain.IncidentStatus;
import com.example.incidentintake.incident.domain.Severity;
import com.example.incidentintake.mcp.IncidentMcpTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentMcpToolsTest {

    @Mock IncidentService incidentService;
    @Mock AuditService auditService;

    IncidentMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new IncidentMcpTools(incidentService, auditService);
    }

    @Test
    void createIncident_delegatesToService() {
        IncidentResponse stub = IncidentResponse.builder()
                .id(UUID.randomUUID()).title("DB down").severity(Severity.HIGH)
                .reportedBy("alice").status(IncidentStatus.OPEN).build();

        when(incidentService.create(any())).thenReturn(new IncidentService.CreateResult(stub, true));

        IncidentResponse result = tools.createIncident("DB down", "HIGH", "alice", null);

        assertThat(result.getTitle()).isEqualTo("DB down");

        ArgumentCaptor<CreateIncidentRequest> captor = ArgumentCaptor.forClass(CreateIncidentRequest.class);
        verify(incidentService).create(captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(Severity.HIGH);
    }

    @Test
    void getIncident_delegatesToService() {
        UUID id = UUID.randomUUID();
        IncidentResponse stub = IncidentResponse.builder().id(id).build();
        when(incidentService.getById(id)).thenReturn(stub);

        IncidentResponse result = tools.getIncident(id.toString());
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void listIncidents_withNullFilters_delegatesNulls() {
        when(incidentService.list(null, null, null)).thenReturn(List.of());
        tools.listIncidents(null, null, null);
        verify(incidentService).list(null, null, null);
    }

    @Test
    void listIncidents_withFilters_parsesEnums() {
        when(incidentService.list(Severity.HIGH, IncidentStatus.OPEN, null)).thenReturn(List.of());
        tools.listIncidents("HIGH", "OPEN", null);
        verify(incidentService).list(Severity.HIGH, IncidentStatus.OPEN, null);
    }

    @Test
    void getIncidentHistory_delegatesToAuditService() {
        UUID id = UUID.randomUUID();
        when(auditService.getHistory(id)).thenReturn(List.of());
        tools.getIncidentHistory(id.toString());
        verify(auditService).getHistory(id);
    }

    @Test
    void updateIncidentStatus_delegatesToService() {
        UUID id = UUID.randomUUID();
        IncidentResponse stub = IncidentResponse.builder()
                .id(id).status(IncidentStatus.IN_PROGRESS).build();
        when(incidentService.updateStatus(eq(id), any())).thenReturn(stub);

        IncidentResponse result = tools.updateIncidentStatus(id.toString(), "IN_PROGRESS", "alice", "working on it");

        assertThat(result.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        verify(incidentService).updateStatus(eq(id), argThat(req ->
                req.getStatus() == IncidentStatus.IN_PROGRESS
                        && "alice".equals(req.getChangedBy())
                        && "working on it".equals(req.getNotes())));
    }

    @Test
    void listIncidents_withOnHoldFilter_parsesEnum() {
        when(incidentService.list(null, IncidentStatus.ON_HOLD, null)).thenReturn(List.of());
        tools.listIncidents(null, "ON_HOLD", null);
        verify(incidentService).list(null, IncidentStatus.ON_HOLD, null);
    }
}
