package com.example.incidentintake.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/{id}/history")
    public List<AuditLogResponse> getHistory(@PathVariable UUID id) {
        return auditService.getHistory(id);
    }
}
