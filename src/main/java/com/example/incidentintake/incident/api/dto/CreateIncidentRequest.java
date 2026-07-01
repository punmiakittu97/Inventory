package com.example.incidentintake.incident.api.dto;

import com.example.incidentintake.incident.domain.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateIncidentRequest {

    @NotBlank(message = "title must not be blank")
    private String title;

    @NotNull(message = "severity must not be null")
    private Severity severity;

    @NotBlank(message = "reportedBy must not be blank")
    private String reportedBy;

    private String externalReferenceId;
}
