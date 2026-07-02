package com.example.incidentintake.audit;

import com.example.incidentintake.incident.domain.IncidentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(String incidentId, IncidentStatus previous, IncidentStatus next,
                       String changedBy, String notes) {
        IncidentAuditLog entry = IncidentAuditLog.builder()
                .incidentId(incidentId)
                .previousStatus(previous)
                .newStatus(next)
                .changedBy(changedBy)
                .changedAt(Instant.now())
                .notes(notes)
                .build();
        auditLogRepository.save(entry);
        log.info("event=AUDIT_RECORD_CREATED incidentId={} from={} to={} changedBy={}",
                incidentId, previous, next, changedBy);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getHistory(String incidentId) {
        List<AuditLogResponse> history = auditLogRepository.findByIncidentIdOrderByChangedAtAsc(incidentId)
                .stream()
                .map(AuditLogResponse::from)
                .collect(Collectors.toList());
        log.debug("event=AUDIT_HISTORY_FETCHED incidentId={} count={}", incidentId, history.size());
        return history;
    }
}
