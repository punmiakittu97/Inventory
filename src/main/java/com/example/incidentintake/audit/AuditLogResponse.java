package com.example.incidentintake.audit;

import com.example.incidentintake.incident.domain.IncidentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AuditLogResponse {

    private UUID id;
    private UUID incidentId;
    private IncidentStatus previousStatus;
    private IncidentStatus newStatus;
    private String changedBy;
    private Instant changedAt;
    private String notes;

    public static AuditLogResponse from(IncidentAuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .incidentId(log.getIncidentId())
                .previousStatus(log.getPreviousStatus())
                .newStatus(log.getNewStatus())
                .changedBy(log.getChangedBy())
                .changedAt(log.getChangedAt())
                .notes(log.getNotes())
                .build();
    }
}
