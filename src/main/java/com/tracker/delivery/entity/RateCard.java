package com.tracker.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rate_cards", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"from_zone_id", "to_zone_id", "order_type"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_zone_id", nullable = false)
    private Zone fromZone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_zone_id", nullable = false)
    private Zone toZone;

    // B2B or B2C
    @Column(name = "order_type", nullable = false)
    private String orderType;

    @Column(nullable = false)
    private Double baseCharge;

    @Column(nullable = false)
    private Double baseWeightKg;

    @Column(nullable = false)
    private Double extraWeightRatePerKg;

    @Column(nullable = false)
    private Double codSurcharge;
}
