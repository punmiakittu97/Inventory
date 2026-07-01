package com.example.incidentintake.resolution;

import java.util.List;
import java.util.UUID;

public record ResolutionSuggestionResponse(
        UUID incidentId,
        String incidentTitle,
        String summary,
        String suggestedResolution,
        List<SimilarIncident> similarIncidents
) {}
