package com.example.incidentintake;

import com.example.incidentintake.incident.infrastructure.IncidentIdGenerator;
import com.example.incidentintake.incident.infrastructure.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentIdGeneratorTest {

    @Mock IncidentRepository incidentRepository;

    @Test
    void next_startsAtInc1001WhenTableIsEmpty() {
        when(incidentRepository.findAllIds()).thenReturn(List.of());

        IncidentIdGenerator generator = new IncidentIdGenerator(incidentRepository);

        assertThat(generator.next()).isEqualTo("INC1001");
    }

    @Test
    void next_incrementsOnEachCall() {
        when(incidentRepository.findAllIds()).thenReturn(List.of());
        IncidentIdGenerator generator = new IncidentIdGenerator(incidentRepository);

        assertThat(generator.next()).isEqualTo("INC1001");
        assertThat(generator.next()).isEqualTo("INC1002");
        assertThat(generator.next()).isEqualTo("INC1003");
    }

    @Test
    void next_resumesAfterHighestExistingId() {
        when(incidentRepository.findAllIds()).thenReturn(List.of("INC1001", "INC1057", "INC1023"));

        IncidentIdGenerator generator = new IncidentIdGenerator(incidentRepository);

        assertThat(generator.next()).isEqualTo("INC1058");
    }

    @Test
    void next_ignoresIdsThatDontMatchThePrefixPattern() {
        when(incidentRepository.findAllIds()).thenReturn(List.of("INC1010", "LEGACY-42", "notanid"));

        IncidentIdGenerator generator = new IncidentIdGenerator(incidentRepository);

        assertThat(generator.next()).isEqualTo("INC1011");
    }

    @Test
    void next_neverReturnsDuplicateAcrossManyCalls() {
        when(incidentRepository.findAllIds()).thenReturn(List.of());
        IncidentIdGenerator generator = new IncidentIdGenerator(incidentRepository);

        List<String> generated = java.util.stream.IntStream.range(0, 500)
                .mapToObj(i -> generator.next())
                .toList();

        assertThat(generated).doesNotHaveDuplicates();
    }
}
