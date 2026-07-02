package com.example.incidentintake.common.exception;

public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException(String id) {
        super("Incident not found: " + id);
    }
}
