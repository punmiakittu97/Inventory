package com.example.incidentintake.common.exception;

import com.example.incidentintake.incident.domain.IncidentStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(IncidentStatus from, IncidentStatus to) {
        super("Invalid status transition from " + from + " to " + to);
    }
}
