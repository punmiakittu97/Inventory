package com.example.incidentintake.resolution;

import com.example.incidentintake.audit.AuditLogResponse;
import com.example.incidentintake.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class IncidentVectorIndexer {

    private final VectorStore vectorStore;
    private final AuditService auditService;

    /**
     * Fires after the RESOLVED transition commits so the audit trail is
     * fully persisted before we read it back for indexing.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIncidentResolved(IncidentResolvedEvent event) {
        List<AuditLogResponse> history = auditService.getHistory(event.incidentId());

        String resolutionNotes = history.stream()
                .filter(a -> a.notes() != null && !a.notes().isBlank())
                .map(AuditLogResponse::notes)
                .collect(Collectors.joining("; "));

        String content = "[%s] %s. Resolution: %s".formatted(
                event.severity(),
                event.title(),
                resolutionNotes.isBlank() ? "no notes recorded" : resolutionNotes);

        Document doc = new Document(content, Map.of(
                "incidentId", event.incidentId().toString(),
                "title",      event.title(),
                "severity",   event.severity(),
                "resolutionNotes", resolutionNotes));

        vectorStore.add(List.of(doc));
        log.info("event=INCIDENT_INDEXED id={} severity={}", event.incidentId(), event.severity());
    }
}
