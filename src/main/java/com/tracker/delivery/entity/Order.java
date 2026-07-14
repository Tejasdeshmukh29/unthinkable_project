package com.tracker.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agent_id")
    private User agent;

    @Column(nullable = false)
    private String pickupAddress;

    @Column(nullable = false)
    private String pickupPincode;

    @Column(nullable = false)
    private String dropAddress;

    @Column(nullable = false)
    private String dropPincode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pickup_zone_id", nullable = false)
    private Zone pickupZone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "drop_zone_id", nullable = false)
    private Zone dropZone;

    @Column(nullable = false)
    private Double lengthCm;

    @Column(nullable = false)
    private Double widthCm;

    @Column(nullable = false)
    private Double heightCm;

    @Column(nullable = false)
    private Double actualWeightKg;

    @Column(nullable = false)
    private Double chargeableWeightKg;

    // B2B or B2C
    @Column(nullable = false)
    private String orderType;

    // PREPAID or COD
    @Column(nullable = false)
    private String paymentType;

    @Column(nullable = false)
    private Double deliveryCharge;

    @Column(nullable = false)
    private Double codSurcharge;

    @Column(nullable = false)
    private Double totalCharge;

    // CREATED, ASSIGNED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, FAILED
    @Column(nullable = false)
    private String status;

    private LocalDateTime scheduledDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (scheduledDate == null) {
            scheduledDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
