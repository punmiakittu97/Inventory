package com.example.incidentintake.resolution;

import java.util.UUID;

public record SimilarIncident(
        UUID incidentId,
        String title,
        String severity,
        String resolutionNotes,
        double similarityScore
) {}
