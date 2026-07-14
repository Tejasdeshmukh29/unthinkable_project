package com.tracker.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {
    @NotBlank
    private String pickupAddress;
    @NotBlank
    private String pickupPincode;
    @NotBlank
    private String dropAddress;
    @NotBlank
    private String dropPincode;
    @NotNull
    private Double lengthCm;
    @NotNull
    private Double widthCm;
    @NotNull
    private Double heightCm;
    @NotNull
    private Double actualWeightKg;
    @NotBlank
    private String orderType; // B2B, B2C
    @NotBlank
    private String paymentType; // PREPAID, COD
    
    private Long customerId; // Nullable (used by Admin to place order on behalf of customer)
}
