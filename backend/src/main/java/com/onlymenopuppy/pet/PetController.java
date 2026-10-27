package com.onlymenopuppy.pet;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pets")
@CrossOrigin(origins = "http://localhost:3000")
public class PetController {
    private final PetService service;
    public PetController(PetService service) { this.service = service; }
    public record Adoption(String name, String species) {}

    @GetMapping("/mine") public Map<String, Object> mine() {
        UUID id = service.findId();
        return id == null ? Map.of("pet", "none") : service.current(id);
    }
    @PostMapping public Map<String, Object> adopt(@RequestBody Adoption body) { return service.adopt(body.name(), body.species()); }
    @GetMapping("/{id}") public Map<String, Object> current(@PathVariable UUID id) { return service.current(id); }
    @GetMapping("/{id}/homecam") public Map<String, Object> homecam(@PathVariable UUID id) { return service.current(id); }
    @GetMapping("/{id}/events") public List<Map<String, Object>> events(@PathVariable UUID id) { return service.events(id); }
    @PostMapping("/{id}/actions/{kind}") public Map<String, Object> act(@PathVariable UUID id, @PathVariable String kind,
        @RequestHeader("Idempotency-Key") String key) { return service.act(id, kind, key); }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(Exception e) { return Map.of("code", "BAD_REQUEST", "message", e.getMessage()); }
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflict(Exception e) { return Map.of("code", e.getMessage(), "message", e.getMessage()); }
}
