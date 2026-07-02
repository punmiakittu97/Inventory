package com.example.incidentintake.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<IncidentAuditLog, UUID> {
    List<IncidentAuditLog> findByIncidentIdOrderByChangedAtAsc(String incidentId);
}
