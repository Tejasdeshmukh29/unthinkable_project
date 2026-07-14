package com.tracker.delivery.service;

import com.tracker.delivery.entity.RateCard;
import com.tracker.delivery.entity.Zone;
import com.tracker.delivery.entity.ZonePincode;
import com.tracker.delivery.repository.RateCardRepository;
import com.tracker.delivery.repository.ZonePincodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RateCalculationServiceTest {

    @Mock
    private ZonePincodeRepository zonePincodeRepository;

    @Mock
    private RateCardRepository rateCardRepository;

    @InjectMocks
    private RateCalculationService rateCalculationService;

    private Zone zoneNorth;
    private Zone zoneWest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        zoneNorth = Zone.builder().id(1L).name("Zone North").description("Delhi").build();
        zoneWest = Zone.builder().id(2L).name("Zone West").description("Mumbai").build();
    }

    @Test
    public void testVolumetricWeightCalculation() {
        // L=30, B=20, H=15 -> 30*20*15 / 5000 = 1.8
        Double volWeight = rateCalculationService.calculateVolumetricWeight(30.0, 20.0, 15.0);
        assertEquals(1.8, volWeight, 0.001);

        // Null dimensions should return 0
        assertEquals(0.0, rateCalculationService.calculateVolumetricWeight(null, 20.0, 15.0));
    }

    @Test
    public void testCalculateIntraZoneB2CPrepaidRate() {
        // Arrange
        String pickupPincode = "110001";
        String dropPincode = "110002";
        
        ZonePincode p1 = ZonePincode.builder().zone(zoneNorth).pincode(pickupPincode).build();
        ZonePincode p2 = ZonePincode.builder().zone(zoneNorth).pincode(dropPincode).build();

        when(zonePincodeRepository.findByPincode(pickupPincode)).thenReturn(Optional.of(p1));
        when(zonePincodeRepository.findByPincode(dropPincode)).thenReturn(Optional.of(p2));

        RateCard rateCard = RateCard.builder()
                .fromZone(zoneNorth)
                .toZone(zoneNorth)
                .orderType("B2C")
                .baseCharge(50.0)
                .baseWeightKg(2.0)
                .extraWeightRatePerKg(10.0)
                .codSurcharge(20.0)
                .build();

        when(rateCardRepository.findByFromZoneAndToZoneAndOrderType(zoneNorth, zoneNorth, "B2C"))
                .thenReturn(Optional.of(rateCard));

        // Act & Assert (Weight <= base limit)
        RateCalculationService.RateCalculationResult result = rateCalculationService.calculate(
                10.0, 10.0, 10.0, 1.5, // vol = 0.2, actual = 1.5 -> chargeable = 1.5
                pickupPincode, dropPincode, "B2C", "PREPAID"
        );

        assertEquals(50.0, result.totalCharge);
        assertEquals(0.0, result.codSurcharge);
        assertEquals(1.5, result.chargeableWeight);

        // Act & Assert (Weight > base limit: 2.5kg)
        // Extra weight = 0.5kg -> ceiled to 1.0kg -> 50 + 1 * 10 = 60
        result = rateCalculationService.calculate(
                10.0, 10.0, 10.0, 2.5, 
                pickupPincode, dropPincode, "B2C", "PREPAID"
        );

        assertEquals(60.0, result.totalCharge);
    }

    @Test
    public void testCalculateInterZoneB2CCODRate() {
        // Arrange
        String pickupPincode = "110001"; // North
        String dropPincode = "400001"; // West

        ZonePincode p1 = ZonePincode.builder().zone(zoneNorth).pincode(pickupPincode).build();
        ZonePincode p2 = ZonePincode.builder().zone(zoneWest).pincode(dropPincode).build();

        when(zonePincodeRepository.findByPincode(pickupPincode)).thenReturn(Optional.of(p1));
        when(zonePincodeRepository.findByPincode(dropPincode)).thenReturn(Optional.of(p2));

        RateCard rateCard = RateCard.builder()
                .fromZone(zoneNorth)
                .toZone(zoneWest)
                .orderType("B2C")
                .baseCharge(120.0)
                .baseWeightKg(2.0)
                .extraWeightRatePerKg(25.0)
                .codSurcharge(20.0)
                .build();

        when(rateCardRepository.findByFromZoneAndToZoneAndOrderType(zoneNorth, zoneWest, "B2C"))
                .thenReturn(Optional.of(rateCard));

        // Act: Actual wt = 1.0kg, Vol wt = 30*20*25/5000 = 3.0kg -> chargeable = 3.0kg
        // Extra weight = 1.0kg -> 120 + 1 * 25 = 145 + 20 (COD) = 165
        RateCalculationService.RateCalculationResult result = rateCalculationService.calculate(
                30.0, 20.0, 25.0, 1.0,
                pickupPincode, dropPincode, "B2C", "COD"
        );

        assertEquals(165.0, result.totalCharge);
        assertEquals(20.0, result.codSurcharge);
        assertEquals(3.0, result.chargeableWeight);
    }
}
