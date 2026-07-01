package com.example.incidentintake.mcp;

import com.example.incidentintake.audit.AuditLogResponse;
import com.example.incidentintake.audit.AuditService;
import com.example.incidentintake.incident.api.dto.CreateIncidentRequest;
import com.example.incidentintake.incident.api.dto.IncidentResponse;
import com.example.incidentintake.incident.api.dto.UpdateStatusRequest;
import com.example.incidentintake.incident.application.IncidentService;
import com.example.incidentintake.incident.domain.IncidentStatus;
import com.example.incidentintake.incident.domain.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Thin MCP adapter — all business logic lives in IncidentService / AuditService.
 * These methods are exposed as MCP tools via Spring AI's tool registration.
 */
@Component
@RequiredArgsConstructor
public class IncidentMcpTools {

    private final IncidentService incidentService;
    private final AuditService auditService;

    @Tool(name = "create_incident",
          description = "Create a new incident. Returns HTTP 200 with the existing incident if the same externalReferenceId was already submitted (idempotency).")
    public IncidentResponse createIncident(
            @ToolParam(description = "Short descriptive title") String title,
            @ToolParam(description = "Severity level: LOW, MEDIUM, HIGH, or CRITICAL") String severity,
            @ToolParam(description = "Name or ID of the person reporting") String reportedBy,
            @ToolParam(description = "Optional external reference ID for idempotency", required = false) String externalReferenceId) {

        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setTitle(title);
        req.setSeverity(Severity.valueOf(severity.toUpperCase()));
        req.setReportedBy(reportedBy);
        req.setExternalReferenceId(externalReferenceId);

        return incidentService.create(req).response();
    }

    @Tool(name = "get_incident",
          description = "Retrieve a single incident by its internal UUID.")
    public IncidentResponse getIncident(
            @ToolParam(description = "Internal UUID of the incident") String id) {
        return incidentService.getById(UUID.fromString(id));
    }

    @Tool(name = "list_incidents",
          description = "List incidents with optional filters. All parameters are optional and combinable.")
    public List<IncidentResponse> listIncidents(
            @ToolParam(description = "Filter by severity: LOW, MEDIUM, HIGH, CRITICAL", required = false) String severity,
            @ToolParam(description = "Filter by status: OPEN, IN_PROGRESS, ON_HOLD, RESOLVED, CLOSED", required = false) String status,
            @ToolParam(description = "Filter by reportedBy", required = false) String reportedBy) {

        Severity sev = severity != null ? Severity.valueOf(severity.toUpperCase()) : null;
        IncidentStatus st = status != null ? IncidentStatus.valueOf(status.toUpperCase()) : null;
        return incidentService.list(sev, st, reportedBy);
    }

    @Tool(name = "update_incident_status",
          description = "Update an incident's status. Allowed transitions: OPEN→IN_PROGRESS|ON_HOLD, IN_PROGRESS→RESOLVED|ON_HOLD, ON_HOLD→IN_PROGRESS|CLOSED, RESOLVED→CLOSED. Invalid transitions return HTTP 422.")
    public IncidentResponse updateIncidentStatus(
            @ToolParam(description = "Internal UUID of the incident") String id,
            @ToolParam(description = "Target status: IN_PROGRESS, ON_HOLD, RESOLVED, or CLOSED") String status,
            @ToolParam(description = "Name or ID of the person making the change") String changedBy,
            @ToolParam(description = "Optional notes about the change", required = false) String notes) {

        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus(IncidentStatus.valueOf(status.toUpperCase()));
        req.setChangedBy(changedBy);
        req.setNotes(notes);
        return incidentService.updateStatus(UUID.fromString(id), req);
    }

    @Tool(name = "get_incident_history",
          description = "Retrieve the full audit trail for an incident, ordered by time ascending.")
    public List<AuditLogResponse> getIncidentHistory(
            @ToolParam(description = "Internal UUID of the incident") String id) {
        return auditService.getHistory(UUID.fromString(id));
    }
}
