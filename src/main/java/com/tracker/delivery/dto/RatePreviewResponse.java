package com.tracker.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatePreviewResponse {
    private String pickupZoneName;
    private String dropZoneName;
    private Double chargeableWeight;
    private Double volumetricWeight;
    private Double deliveryCharge;
    private Double codSurcharge;
    private Double totalCharge;
}
