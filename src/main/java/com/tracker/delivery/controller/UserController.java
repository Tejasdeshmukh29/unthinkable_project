package com.tracker.delivery.controller;

import com.tracker.delivery.entity.User;
import com.tracker.delivery.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/agents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllAgents() {
        return ResponseEntity.ok(userService.getAllAgents());
    }

    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllCustomers() {
        return ResponseEntity.ok(userService.getAllCustomers());
    }

    @PutMapping("/agents/{id}/location")
    public ResponseEntity<?> updateAgentLocation(
            @PathVariable Long id,
            @RequestBody Map<String, Double> coordinates) {
        try {
            Double lat = coordinates.get("latitude");
            Double lon = coordinates.get("longitude");
            if (lat == null || lon == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Latitude and Longitude are required"));
            }
            User agent = userService.updateAgentLocation(id, lat, lon);
            return ResponseEntity.ok(agent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/agents/{id}/status")
    public ResponseEntity<?> updateAgentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
            }
            User agent = userService.updateAgentStatus(id, status);
            return ResponseEntity.ok(agent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
