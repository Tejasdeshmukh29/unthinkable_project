package com.tracker.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zone_pincodes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZonePincode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(nullable = false, unique = true)
    private String pincode;
}
