package com.example.incidentintake.incident.infrastructure;

import com.example.incidentintake.incident.domain.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID>,
        JpaSpecificationExecutor<Incident> {

    Optional<Incident> findByExternalReferenceId(String externalReferenceId);
}
