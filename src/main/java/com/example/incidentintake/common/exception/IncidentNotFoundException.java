package com.example.incidentintake.common.exception;

import java.util.UUID;

public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException(UUID id) {
        super("Incident not found: " + id);
    }
}
