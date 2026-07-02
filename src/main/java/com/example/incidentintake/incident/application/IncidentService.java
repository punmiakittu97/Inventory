package com.example.incidentintake.incident.application;

import com.example.incidentintake.audit.AuditService;
import com.example.incidentintake.common.exception.IncidentNotFoundException;
import com.example.incidentintake.common.exception.InvalidStatusTransitionException;
import com.example.incidentintake.incident.api.dto.CreateIncidentRequest;
import com.example.incidentintake.incident.api.dto.IncidentResponse;
import com.example.incidentintake.incident.api.dto.UpdateStatusRequest;
import com.example.incidentintake.incident.domain.Incident;
import com.example.incidentintake.incident.domain.IncidentStatus;
import com.example.incidentintake.incident.domain.Severity;
import com.example.incidentintake.incident.infrastructure.IncidentIdGenerator;
import com.example.incidentintake.incident.infrastructure.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AuditService auditService;
    private final IncidentIdGenerator idGenerator;

    /**
     * Idempotent creation: if externalReferenceId is supplied and an incident already exists
     * with that value, the existing incident is returned rather than creating a duplicate.
     * Callers can distinguish create (HTTP 201) from a hit (HTTP 200) via the ResponseEntity
     * status set in the controller.
     */
    @Transactional
    public CreateResult create(CreateIncidentRequest req) {
        if (req.getExternalReferenceId() != null) {
            Optional<Incident> existing =
                    incidentRepository.findByExternalReferenceId(req.getExternalReferenceId());
            if (existing.isPresent()) {
                log.info("event=INCIDENT_IDEMPOTENT_HIT externalReferenceId={}", req.getExternalReferenceId());
                return new CreateResult(IncidentResponse.from(existing.get()), false);
            }
        }

        Incident incident = Incident.builder()
                .id(idGenerator.next())
                .title(req.getTitle())
                .severity(req.getSeverity())
                .reportedBy(req.getReportedBy())
                .externalReferenceId(req.getExternalReferenceId())
                .build();

        incident = incidentRepository.save(incident);

        try {
            MDC.put("incidentId", incident.getId());
            if (incident.getSeverity() == Severity.CRITICAL) {
                log.warn("event=INCIDENT_CREATED id={} severity=CRITICAL title=\"{}\" reportedBy={}",
                        incident.getId(), incident.getTitle(), incident.getReportedBy());
            } else {
                log.info("event=INCIDENT_CREATED id={} severity={} title=\"{}\" reportedBy={}",
                        incident.getId(), incident.getSeverity(), incident.getTitle(), incident.getReportedBy());
            }
        } finally {
            MDC.remove("incidentId");
        }

        return new CreateResult(IncidentResponse.from(incident), true);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getById(String id) {
        IncidentResponse response = incidentRepository.findById(id)
                .map(IncidentResponse::from)
                .orElseThrow(() -> new IncidentNotFoundException(id));
        log.debug("event=INCIDENT_FETCHED id={}", id);
        return response;
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> list(Severity severity, IncidentStatus status, String reportedBy) {
        log.debug("event=INCIDENT_LIST_QUERY severity={} status={} reportedBy={}", severity, status, reportedBy);
        Specification<Incident> spec = Specification.where(
                severityEquals(severity))
                .and(statusEquals(status))
                .and(reportedByEquals(reportedBy));

        List<IncidentResponse> results = incidentRepository.findAll(spec)
                .stream()
                .map(IncidentResponse::from)
                .collect(Collectors.toList());
        log.debug("event=INCIDENT_LIST_RESULT count={}", results.size());
        return results;
    }

    @Transactional
    public IncidentResponse updateStatus(String id, UpdateStatusRequest req) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));

        IncidentStatus previous = incident.getStatus();
        IncidentStatus next = req.getStatus();

        if (!isValidTransition(previous, next)) {
            throw new InvalidStatusTransitionException(previous, next);
        }

        incident.setStatus(next);
        if (next == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(Instant.now());
        }
        incidentRepository.save(incident);

        auditService.record(id, previous, next, req.getChangedBy(), req.getNotes());

        try {
            MDC.put("incidentId", id);
            log.info("event=INCIDENT_STATUS_CHANGED id={} from={} to={} changedBy={}",
                    id, previous, next, req.getChangedBy());
        } finally {
            MDC.remove("incidentId");
        }

        return IncidentResponse.from(incident);
    }

    // Transitions: OPEN→IN_PROGRESS|ON_HOLD, IN_PROGRESS→RESOLVED|ON_HOLD,
    //              ON_HOLD→IN_PROGRESS|CLOSED, RESOLVED→CLOSED
    private boolean isValidTransition(IncidentStatus from, IncidentStatus to) {
        return switch (from) {
            case OPEN        -> to == IncidentStatus.IN_PROGRESS || to == IncidentStatus.ON_HOLD;
            case IN_PROGRESS -> to == IncidentStatus.RESOLVED    || to == IncidentStatus.ON_HOLD;
            case ON_HOLD     -> to == IncidentStatus.IN_PROGRESS || to == IncidentStatus.CLOSED;
            case RESOLVED    -> to == IncidentStatus.CLOSED;
            case CLOSED      -> false;
        };
    }

    private Specification<Incident> severityEquals(Severity severity) {
        return (root, query, cb) ->
                severity == null ? null : cb.equal(root.get("severity"), severity);
    }

    private Specification<Incident> statusEquals(IncidentStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    private Specification<Incident> reportedByEquals(String reportedBy) {
        return (root, query, cb) ->
                reportedBy == null ? null : cb.equal(root.get("reportedBy"), reportedBy);
    }

    public record CreateResult(IncidentResponse response, boolean created) {}
}
