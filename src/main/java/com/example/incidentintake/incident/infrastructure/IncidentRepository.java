package com.example.incidentintake.incident.infrastructure;

import com.example.incidentintake.incident.domain.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, String>,
        JpaSpecificationExecutor<Incident> {

    Optional<Incident> findByExternalReferenceId(String externalReferenceId);

    @Query("SELECT i.id FROM Incident i")
    List<String> findAllIds();
}
