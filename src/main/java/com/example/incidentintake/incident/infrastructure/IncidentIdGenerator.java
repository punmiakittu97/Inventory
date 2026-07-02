package com.example.incidentintake.incident.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Produces human-readable incident IDs like INC1001, INC1002, ... instead of UUIDs.
 * Seeded at startup from the highest existing numeric suffix so restarts never
 * reissue an ID already in use.
 */
@Component
@Slf4j
public class IncidentIdGenerator {

    static final String PREFIX = "INC";
    private static final long START = 1000L;
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("^" + PREFIX + "(\\d+)$");

    private final AtomicLong counter;

    public IncidentIdGenerator(IncidentRepository incidentRepository) {
        long highestExisting = incidentRepository.findAllIds().stream()
                .map(SUFFIX_PATTERN::matcher)
                .filter(Matcher::matches)
                .mapToLong(m -> Long.parseLong(m.group(1)))
                .max()
                .orElse(START);
        this.counter = new AtomicLong(highestExisting);
        log.info("event=INCIDENT_ID_GENERATOR_INITIALIZED nextId={}{}", PREFIX, highestExisting + 1);
    }

    public String next() {
        return PREFIX + counter.incrementAndGet();
    }
}
