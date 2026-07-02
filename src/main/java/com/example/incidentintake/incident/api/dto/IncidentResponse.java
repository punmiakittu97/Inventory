package com.example.incidentintake.incident.api.dto;

import com.example.incidentintake.incident.domain.Incident;
import com.example.incidentintake.incident.domain.IncidentStatus;
import com.example.incidentintake.incident.domain.Severity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class IncidentResponse {

    private String id;
    private String title;
    private Severity severity;
    private String reportedBy;
    private String externalReferenceId;
    private IncidentStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;

    public static IncidentResponse from(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .severity(incident.getSeverity())
                .reportedBy(incident.getReportedBy())
                .externalReferenceId(incident.getExternalReferenceId())
                .status(incident.getStatus())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .resolvedAt(incident.getResolvedAt())
                .build();
    }
}
