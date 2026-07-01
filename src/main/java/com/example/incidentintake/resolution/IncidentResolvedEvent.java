package com.example.incidentintake.resolution;

import java.util.UUID;

public record IncidentResolvedEvent(UUID incidentId, String title, String severity) {}
