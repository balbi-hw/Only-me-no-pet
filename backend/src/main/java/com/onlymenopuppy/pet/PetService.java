package com.onlymenopuppy.pet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PetService {
    private static final String OWNER = "demo-user";
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final Clock clock = Clock.systemUTC();
    private final Simulation simulation = new Simulation();

    public PetService(JdbcTemplate db, ObjectMapper json) { this.db = db; this.json = json; }

    @Transactional
    public Map<String, Object> adopt(String name, String species) {
        if (name == null || name.isBlank() || name.length() > 30) throw new IllegalArgumentException("이름은 1~30자여야 합니다");
        if (!List.of("DOG", "CAT").contains(species)) throw new IllegalArgumentException("DOG 또는 CAT을 선택하세요");
        if (findId() != null) throw new IllegalStateException("ALREADY_ADOPTED");
        UUID id = UUID.randomUUID();
        Instant now = clock.instant();
        db.update("INSERT INTO pets(id,owner_id,name,species) VALUES (?,?,?,?)", id, OWNER, name.trim(), species);
        db.update("INSERT INTO pet_state(pet_id,hunger,energy,mood,bond,stress,action,simulated_until) VALUES (?,?,?,?,?,?,?,?)",
            id, 35, 75, 70, 30, 15, "REST", Timestamp.from(now));
        db.update("INSERT INTO pet_events(pet_id,occurred_at,event_type,detail) VALUES (?,?,?,?)", id, Timestamp.from(now), "ADOPTED", "새 가족이 되었어요");
        return current(id);
    }

    public UUID findId() {
        List<UUID> ids = db.query("SELECT id FROM pets WHERE owner_id=?", (rs, n) -> rs.getObject(1, UUID.class), OWNER);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    @Transactional
    public Map<String, Object> current(UUID id) {
        verifyOwner(id);
        PetState state = lockedState(id);
        Simulation.Result result = simulation.restore(state, clock.instant());
        persist(result);
        Map<String, Object> pet = db.queryForMap("SELECT id,name,species,created_at FROM pets WHERE id=?", id);
        return Map.of("pet", pet, "state", result.state(), "sceneState", Map.of(
            "room", "LIVING_ROOM", "zone", "FLOOR", "action", result.state().action(),
            "species", pet.get("species")), "asOf", result.state().simulatedUntil());
    }

    @Transactional
    public List<Map<String, Object>> events(UUID id) {
        current(id);
        return db.queryForList("SELECT occurred_at,event_type,detail FROM pet_events WHERE pet_id=? ORDER BY occurred_at DESC,id DESC LIMIT 30", id);
    }

    @Transactional
    public Map<String, Object> act(UUID id, String kind, String key) {
        if (key == null || key.isBlank() || key.length() > 100) throw new IllegalArgumentException("Idempotency-Key가 필요합니다");
        verifyOwner(id);
        String requestHash = id + ":" + kind;
        List<Map<String, Object>> existing = db.queryForList("SELECT request_hash,response_json FROM action_requests WHERE owner_id=? AND idempotency_key=?", OWNER, key);
        if (!existing.isEmpty()) return replay(existing.getFirst(), requestHash);
        // The pet row serializes all commands for this pet, including simultaneous retries.
        db.queryForObject("SELECT id FROM pets WHERE id=? FOR UPDATE", UUID.class, id);
        existing = db.queryForList("SELECT request_hash,response_json FROM action_requests WHERE owner_id=? AND idempotency_key=?", OWNER, key);
        if (!existing.isEmpty()) return replay(existing.getFirst(), requestHash);
        Simulation.Result restoration = simulation.restore(lockedState(id), clock.instant());
        PetState restored = restoration.state();
        persist(restoration);
        Simulation.Result result = simulation.interact(restored, kind, clock.instant());
        persist(result);
        Map<String, Object> response = Map.of("state", result.state(), "event", result.events().getFirst());
        try {
            db.update("INSERT INTO action_requests(owner_id,idempotency_key,request_hash,response_json) VALUES (?,?,?,?)",
                OWNER, key, requestHash, json.writeValueAsString(response));
        } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
        return response;
    }

    private Map<String, Object> replay(Map<String, Object> row, String requestHash) {
        if (!requestHash.equals(row.get("request_hash"))) throw new IllegalStateException("IDEMPOTENCY_CONFLICT");
        try { return json.readValue((String) row.get("response_json"), Map.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    private void verifyOwner(UUID id) {
        Integer count = db.queryForObject("SELECT count(*) FROM pets WHERE id=? AND owner_id=?", Integer.class, id, OWNER);
        if (count == null || count == 0) throw new IllegalStateException("PET_NOT_FOUND");
    }

    private PetState lockedState(UUID id) {
        return db.queryForObject("SELECT * FROM pet_state WHERE pet_id=? FOR UPDATE", (rs, n) -> readState(rs), id);
    }

    private PetState readState(ResultSet rs) throws SQLException {
        return new PetState(rs.getObject("pet_id", UUID.class), rs.getInt("hunger"), rs.getInt("energy"),
            rs.getInt("mood"), rs.getInt("bond"), rs.getInt("stress"), rs.getString("action"),
            rs.getTimestamp("simulated_until").toInstant(), rs.getLong("version"));
    }

    private void persist(Simulation.Result result) {
        PetState s = result.state();
        db.update("UPDATE pet_state SET hunger=?,energy=?,mood=?,bond=?,stress=?,action=?,simulated_until=?,version=? WHERE pet_id=?",
            s.hunger(), s.energy(), s.mood(), s.bond(), s.stress(), s.action(), Timestamp.from(s.simulatedUntil()), s.version(), s.petId());
        for (Simulation.Event e : result.events())
            db.update("INSERT INTO pet_events(pet_id,occurred_at,event_type,detail) VALUES (?,?,?,?)", s.petId(), Timestamp.from(e.at()), e.type(), e.detail());
    }
}
