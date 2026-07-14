package com.tracker.delivery.controller;

import com.tracker.delivery.entity.Zone;
import com.tracker.delivery.entity.ZonePincode;
import com.tracker.delivery.repository.ZonePincodeRepository;
import com.tracker.delivery.repository.ZoneRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final ZoneRepository zoneRepository;
    private final ZonePincodeRepository pincodeRepository;

    public ZoneController(ZoneRepository zoneRepository, ZonePincodeRepository pincodeRepository) {
        this.zoneRepository = zoneRepository;
        this.pincodeRepository = pincodeRepository;
    }

    @GetMapping
    public ResponseEntity<List<Zone>> getZones() {
        return ResponseEntity.ok(zoneRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createZone(@RequestBody Zone zone) {
        if (zoneRepository.findByName(zone.getName()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Zone name already exists"));
        }
        return ResponseEntity.ok(zoneRepository.save(zone));
    }

    @PostMapping("/{id}/pincodes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addPincode(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found"));

        String pincode = request.get("pincode");
        if (pincode == null || pincode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Pincode is required"));
        }

        if (pincodeRepository.existsByPincode(pincode)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Pincode already mapped to a zone"));
        }

        ZonePincode zonePincode = ZonePincode.builder()
                .zone(zone)
                .pincode(pincode)
                .build();

        return ResponseEntity.ok(pincodeRepository.save(zonePincode));
    }
}
