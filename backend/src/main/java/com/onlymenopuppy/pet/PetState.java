package com.onlymenopuppy.pet;

import java.time.Instant;
import java.util.UUID;

public record PetState(UUID petId, int hunger, int energy, int mood, int bond, int stress,
                       String action, Instant simulatedUntil, long version) {
    public PetState advance(int hunger, int energy, int mood, int bond, int stress, String action, Instant time) {
        return new PetState(petId, clamp(hunger), clamp(energy), clamp(mood), clamp(bond), clamp(stress), action, time, version + 1);
    }
    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
}
