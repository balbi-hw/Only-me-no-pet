package com.onlymenopuppy.pet;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Fixed one-hour steps keep offline restoration bounded and reproducible. */
public class Simulation {
    public record Event(Instant at, String type, String detail) {}
    public record Result(PetState state, List<Event> events) {}

    public Result restore(PetState initial, Instant now) {
        if (!now.isAfter(initial.simulatedUntil())) return new Result(initial, List.of());
        PetState state = initial;
        List<Event> events = new ArrayList<>();
        long hours = Math.min(168, Duration.between(state.simulatedUntil(), now).toHours());
        for (long i = 0; i < hours; i++) {
            Instant at = state.simulatedUntil().plus(Duration.ofHours(1));
            int hunger = state.hunger() + 3;
            int energy = state.energy();
            int mood = state.mood();
            int stress = state.stress();
            String action;
            if (energy < 25) { action = "SLEEP"; energy += 18; stress -= 2; }
            else if (hunger > 70) { action = "LOOKING_FOR_FOOD"; mood -= 2; stress += 2; energy -= 2; }
            else {
                int pick = Math.floorMod(state.petId().hashCode() + at.getEpochSecond() / 3600, 3);
                action = switch (pick) { case 0 -> "EXPLORE"; case 1 -> "PLAY_ALONE"; default -> "REST"; };
                energy += action.equals("REST") ? 5 : -5;
                mood += action.equals("PLAY_ALONE") ? 3 : 0;
            }
            if (!action.equals(state.action())) events.add(new Event(at, action, "스스로 " + label(action)));
            state = state.advance(hunger, energy, mood, state.bond(), stress, action, at);
        }
        // A long absence is summarized; no unbounded event replay or endless deterioration.
        if (state.simulatedUntil().isBefore(now)) {
            if (Duration.between(state.simulatedUntil(), now).toHours() > 0)
                events.add(new Event(now, "LONG_ABSENCE", "오랜 부재 동안 쉬고 탐색했어요"));
            state = state.advance(state.hunger(), state.energy(), state.mood(), state.bond(), state.stress(), state.action(), now);
        }
        return new Result(state, events);
    }

    public Result interact(PetState state, String kind, Instant now) {
        if (kind.equals("play") && state.energy() < 15) throw new IllegalStateException("TOO_TIRED");
        if (kind.equals("train") && (state.energy() < 20 || state.stress() > 80)) throw new IllegalStateException("NOT_READY");
        if (kind.equals("feed") && state.hunger() < 15) throw new IllegalStateException("NOT_HUNGRY");
        PetState next = switch (kind) {
            case "feed" -> state.advance(state.hunger() - 28, state.energy(), state.mood() + 3, state.bond() + 2, state.stress(), "EATING", now);
            case "play" -> state.advance(state.hunger() + 4, state.energy() - 12, state.mood() + 15, state.bond() + 8, state.stress() - 5, "PLAYING_TOGETHER", now);
            case "train" -> state.advance(state.hunger() + 2, state.energy() - 8, state.mood() + 2, state.bond() + 6, state.stress() + 2, "TRAINING", now);
            default -> throw new IllegalArgumentException("알 수 없는 행동입니다");
        };
        return new Result(next, List.of(new Event(now, kind.toUpperCase(), switch (kind) {
            case "feed" -> "밥을 맛있게 먹었어요"; case "play" -> "함께 놀았어요"; default -> "기초 훈련을 했어요";
        })));
    }

    private String label(String action) { return switch (action) {
        case "SLEEP" -> "잠들었어요"; case "EXPLORE" -> "집을 탐색했어요";
        case "PLAY_ALONE" -> "장난감을 가지고 놀았어요"; case "REST" -> "쉬었어요";
        default -> "먹이를 찾았어요";
    }; }
}
