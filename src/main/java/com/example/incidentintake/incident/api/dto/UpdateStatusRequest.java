package com.example.incidentintake.incident.api.dto;

import com.example.incidentintake.incident.domain.IncidentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "status must not be null")
    private IncidentStatus status;

    @NotBlank(message = "changedBy must not be blank")
    private String changedBy;

    private String notes;
}
