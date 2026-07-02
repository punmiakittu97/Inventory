package com.example.incidentintake.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/{id}/history")
    public List<AuditLogResponse> getHistory(@PathVariable String id) {
        return auditService.getHistory(id);
    }
}
