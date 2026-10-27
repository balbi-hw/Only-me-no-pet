package com.onlymenopuppy.pet;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SimulationTest {
    private final Simulation simulation = new Simulation();
    private final Instant start = Instant.parse("2026-10-27T00:00:00Z");
    private final PetState initial = new PetState(UUID.fromString("00000000-0000-0000-0000-000000000001"),
        35, 75, 70, 30, 15, "REST", start, 0);

    @Test void restorationIsDeterministicAndMonotonic() {
        Instant end = start.plus(Duration.ofHours(24));
        Simulation.Result first = simulation.restore(initial, end);
        Simulation.Result second = simulation.restore(initial, end);
        assertEquals(first, second);
        assertEquals(end, first.state().simulatedUntil());
        assertEquals(first.state(), simulation.restore(first.state(), end).state());
        assertTrue(first.state().hunger() <= 100);
        assertTrue(first.state().energy() >= 0);
    }

    @Test void longAbsenceStaysBounded() {
        Simulation.Result result = simulation.restore(initial, start.plus(Duration.ofDays(30)));
        assertTrue(result.events().size() <= 169);
        assertEquals(start.plus(Duration.ofDays(30)), result.state().simulatedUntil());
    }

    @Test void tiredPetRejectsPlay() {
        PetState tired = initial.advance(35, 10, 70, 30, 15, "REST", start);
        assertEquals("TOO_TIRED", assertThrows(IllegalStateException.class,
            () -> simulation.interact(tired, "play", start)).getMessage());
    }
}
