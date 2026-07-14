package com.tracker.delivery.controller;

import com.tracker.delivery.dto.RatePreviewRequest;
import com.tracker.delivery.dto.RatePreviewResponse;
import com.tracker.delivery.entity.RateCard;
import com.tracker.delivery.entity.Zone;
import com.tracker.delivery.repository.RateCardRepository;
import com.tracker.delivery.repository.ZoneRepository;
import com.tracker.delivery.service.RateCalculationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/rates")
public class RateCardController {

    private final RateCardRepository rateCardRepository;
    private final ZoneRepository zoneRepository;
    private final RateCalculationService rateCalculationService;

    public RateCardController(
            RateCardRepository rateCardRepository,
            ZoneRepository zoneRepository,
            RateCalculationService rateCalculationService) {
        this.rateCardRepository = rateCardRepository;
        this.zoneRepository = zoneRepository;
        this.rateCalculationService = rateCalculationService;
    }

    @GetMapping
    public ResponseEntity<List<RateCard>> getRateCards() {
        return ResponseEntity.ok(rateCardRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> configureRateCard(@RequestBody RateCard request) {
        try {
            Zone from = zoneRepository.findById(request.getFromZone().getId())
                    .orElseThrow(() -> new IllegalArgumentException("From Zone not found"));
            Zone to = zoneRepository.findById(request.getToZone().getId())
                    .orElseThrow(() -> new IllegalArgumentException("To Zone not found"));

            Optional<RateCard> existing = rateCardRepository.findByFromZoneAndToZoneAndOrderType(
                    from, to, request.getOrderType());

            RateCard rateCard;
            if (existing.isPresent()) {
                rateCard = existing.get();
                rateCard.setBaseCharge(request.getBaseCharge());
                rateCard.setBaseWeightKg(request.getBaseWeightKg());
                rateCard.setExtraWeightRatePerKg(request.getExtraWeightRatePerKg());
                rateCard.setCodSurcharge(request.getCodSurcharge());
            } else {
                rateCard = RateCard.builder()
                        .fromZone(from)
                        .toZone(to)
                        .orderType(request.getOrderType())
                        .baseCharge(request.getBaseCharge())
                        .baseWeightKg(request.getBaseWeightKg())
                        .extraWeightRatePerKg(request.getExtraWeightRatePerKg())
                        .codSurcharge(request.getCodSurcharge())
                        .build();
            }

            return ResponseEntity.ok(rateCardRepository.save(rateCard));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/preview")
    public ResponseEntity<?> previewRate(@Valid @RequestBody RatePreviewRequest request) {
        try {
            RateCalculationService.RateCalculationResult result = rateCalculationService.calculate(
                    request.getLengthCm(),
                    request.getWidthCm(),
                    request.getHeightCm(),
                    request.getActualWeightKg(),
                    request.getPickupPincode(),
                    request.getDropPincode(),
                    request.getOrderType(),
                    request.getPaymentType()
            );

            RatePreviewResponse response = RatePreviewResponse.builder()
                    .pickupZoneName(result.pickupZone.getName())
                    .dropZoneName(result.dropZone.getName())
                    .volumetricWeight(result.volumetricWeight)
                    .chargeableWeight(result.chargeableWeight)
                    .deliveryCharge(result.deliveryCharge)
                    .codSurcharge(result.codSurcharge)
                    .totalCharge(result.totalCharge)
                    .build();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
