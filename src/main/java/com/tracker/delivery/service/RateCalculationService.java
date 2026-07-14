package com.tracker.delivery.service;

import com.tracker.delivery.entity.RateCard;
import com.tracker.delivery.entity.Zone;
import com.tracker.delivery.entity.ZonePincode;
import com.tracker.delivery.repository.RateCardRepository;
import com.tracker.delivery.repository.ZonePincodeRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class RateCalculationService {

    private final ZonePincodeRepository zonePincodeRepository;
    private final RateCardRepository rateCardRepository;

    public RateCalculationService(ZonePincodeRepository zonePincodeRepository, RateCardRepository rateCardRepository) {
        this.zonePincodeRepository = zonePincodeRepository;
        this.rateCardRepository = rateCardRepository;
    }

    public Zone detectZone(String pincode) {
        return zonePincodeRepository.findByPincode(pincode)
                .map(ZonePincode::getZone)
                .orElseThrow(() -> new IllegalArgumentException("Service not available for pincode: " + pincode));
    }

    public Double calculateVolumetricWeight(Double length, Double width, Double height) {
        if (length == null || width == null || height == null) {
            return 0.0;
        }
        // L * B * H / 5000
        return (length * width * height) / 5000.0;
    }

    public static class RateCalculationResult {
        public Double chargeableWeight;
        public Double volumetricWeight;
        public Zone pickupZone;
        public Zone dropZone;
        public Double deliveryCharge;
        public Double codSurcharge;
        public Double totalCharge;
    }

    public RateCalculationResult calculate(
            Double length, Double width, Double height, Double actualWeight,
            String pickupPincode, String dropPincode,
            String orderType, String paymentType) {

        if (actualWeight == null || actualWeight <= 0) {
            throw new IllegalArgumentException("Actual weight must be greater than 0");
        }

        RateCalculationResult result = new RateCalculationResult();

        // 1. Detect zones
        result.pickupZone = detectZone(pickupPincode);
        result.dropZone = detectZone(dropPincode);

        // 2. Volumetric weight
        result.volumetricWeight = calculateVolumetricWeight(length, width, height);
        result.chargeableWeight = Math.max(actualWeight, result.volumetricWeight);

        // 3. Look up Rate Card
        Optional<RateCard> rateCardOpt = rateCardRepository.findByFromZoneAndToZoneAndOrderType(
                result.pickupZone, result.dropZone, orderType);

        // Fallback: If no direct rate card exists, look up intra-zone of pickup zone as a default base,
        // or check for default. We will throw an error if no rate card is configured.
        if (rateCardOpt.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "No rate configuration found from %s to %s for type %s",
                    result.pickupZone.getName(), result.dropZone.getName(), orderType));
        }

        RateCard rateCard = rateCardOpt.get();

        // 4. Calculate Delivery Charge
        double deliveryCharge = rateCard.getBaseCharge();
        if (result.chargeableWeight > rateCard.getBaseWeightKg()) {
            double extraWeight = result.chargeableWeight - rateCard.getBaseWeightKg();
            deliveryCharge += Math.ceil(extraWeight) * rateCard.getExtraWeightRatePerKg();
        }
        result.deliveryCharge = deliveryCharge;

        // 5. Calculate COD Surcharge
        double codSurcharge = 0.0;
        if ("COD".equalsIgnoreCase(paymentType)) {
            codSurcharge = rateCard.getCodSurcharge();
        }
        result.codSurcharge = codSurcharge;

        // 6. Total Charge
        result.totalCharge = result.deliveryCharge + result.codSurcharge;

        return result;
    }
}
