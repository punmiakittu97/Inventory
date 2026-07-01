package com.example.incidentintake.resolution;

import com.example.incidentintake.audit.AuditLogResponse;
import com.example.incidentintake.audit.AuditService;
import com.example.incidentintake.incident.api.dto.IncidentResponse;
import com.example.incidentintake.incident.application.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResolutionService {

    private static final int TOP_K = 3;

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final IncidentService incidentService;
    private final AuditService auditService;

    public ResolutionSuggestionResponse suggest(UUID incidentId) {
        IncidentResponse incident = incidentService.getById(incidentId);
        List<AuditLogResponse> history = auditService.getHistory(incidentId);

        String historySummary = history.stream()
                .map(a -> "  %s → %s (by %s)%s".formatted(
                        a.previousStatus(), a.newStatus(), a.changedBy(),
                        a.notes() != null && !a.notes().isBlank() ? ": " + a.notes() : ""))
                .collect(Collectors.joining("\n"));

        String summary = "[%s] %s | Status: %s | Reported by: %s\nTimeline:\n%s".formatted(
                incident.severity(), incident.title(), incident.status(), incident.reportedBy(),
                historySummary.isBlank() ? "  (no history yet)" : historySummary);

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("[%s] %s".formatted(incident.severity(), incident.title()))
                        .topK(TOP_K)
                        .build());

        List<SimilarIncident> similar = docs.stream()
                .map(doc -> new SimilarIncident(
                        UUID.fromString(doc.getMetadata().get("incidentId").toString()),
                        doc.getMetadata().get("title").toString(),
                        doc.getMetadata().get("severity").toString(),
                        doc.getMetadata().getOrDefault("resolutionNotes", "").toString(),
                        0.0))
                .collect(Collectors.toList());

        String suggestion = similar.isEmpty()
                ? "No similar resolved incidents in the knowledge base yet. Investigate manually."
                : callLlm(summary, similar);

        log.info("event=RESOLUTION_SUGGESTED incidentId={} similarCount={}", incidentId, similar.size());

        return new ResolutionSuggestionResponse(incidentId, incident.title(), summary, suggestion, similar);
    }

    private String callLlm(String currentSummary, List<SimilarIncident> similar) {
        String similarSection = similar.stream()
                .map(s -> "- [%s] %s\n  Resolution: %s".formatted(
                        s.severity(), s.title(),
                        s.resolutionNotes().isBlank() ? "no notes recorded" : s.resolutionNotes()))
                .collect(Collectors.joining("\n"));

        String promptText = """
                You are an incident response assistant. A new incident has been reported. \
                Using the similar resolved incidents below as context, provide a concise, \
                actionable resolution plan (3-5 sentences).

                CURRENT INCIDENT:
                %s

                SIMILAR RESOLVED INCIDENTS:
                %s

                Provide only the suggested resolution steps. Be specific and actionable.
                """.formatted(currentSummary, similarSection);

        return chatModel.call(new Prompt(promptText))
                .getResult()
                .getOutput()
                .getContent();
    }
}
