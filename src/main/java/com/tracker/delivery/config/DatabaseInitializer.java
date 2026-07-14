package com.tracker.delivery.config;

import com.tracker.delivery.entity.*;
import com.tracker.delivery.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;
    private final ZonePincodeRepository pincodeRepository;
    private final RateCardRepository rateCardRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(
            UserRepository userRepository,
            ZoneRepository zoneRepository,
            ZonePincodeRepository pincodeRepository,
            RateCardRepository rateCardRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.zoneRepository = zoneRepository;
        this.pincodeRepository = pincodeRepository;
        this.rateCardRepository = rateCardRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // If DB is already seeded, skip
        if (userRepository.findByUsername("admin").isPresent()) {
            return;
        }

        System.out.println("Seeding database with default logistics configurations...");

        // 1. Create Zones
        Zone zoneNorth = zoneRepository.save(Zone.builder().name("Zone North").description("Delhi & NCR Region").build());
        Zone zoneWest = zoneRepository.save(Zone.builder().name("Zone West").description("Mumbai & Maharashtra Region").build());
        Zone zoneSouth = zoneRepository.save(Zone.builder().name("Zone South").description("Bangalore & Karnataka Region").build());
        Zone zoneEast = zoneRepository.save(Zone.builder().name("Zone East").description("Kolkata & West Bengal Region").build());

        // 2. Map Pincodes
        pincodeRepository.save(ZonePincode.builder().zone(zoneNorth).pincode("110001").build());
        pincodeRepository.save(ZonePincode.builder().zone(zoneNorth).pincode("110002").build());

        pincodeRepository.save(ZonePincode.builder().zone(zoneWest).pincode("400001").build());
        pincodeRepository.save(ZonePincode.builder().zone(zoneWest).pincode("400002").build());

        pincodeRepository.save(ZonePincode.builder().zone(zoneSouth).pincode("560001").build());
        pincodeRepository.save(ZonePincode.builder().zone(zoneSouth).pincode("600001").build());

        pincodeRepository.save(ZonePincode.builder().zone(zoneEast).pincode("700001").build());

        // 3. Create Rate Cards (All Permutations between 4 Zones)
        List<Zone> allZones = List.of(zoneNorth, zoneWest, zoneSouth, zoneEast);
        List<RateCard> rateCards = new ArrayList<>();

        for (Zone from : allZones) {
            for (Zone to : allZones) {
                boolean isIntra = from.getId().equals(to.getId());

                // B2C Rate Configuration
                rateCards.add(RateCard.builder()
                        .fromZone(from)
                        .toZone(to)
                        .orderType("B2C")
                        .baseCharge(isIntra ? 50.0 : 120.0)
                        .baseWeightKg(2.0)
                        .extraWeightRatePerKg(isIntra ? 10.0 : 25.0)
                        .codSurcharge(20.0)
                        .build());

                // B2B Rate Configuration
                rateCards.add(RateCard.builder()
                        .fromZone(from)
                        .toZone(to)
                        .orderType("B2B")
                        .baseCharge(isIntra ? 150.0 : 350.0)
                        .baseWeightKg(5.0)
                        .extraWeightRatePerKg(isIntra ? 15.0 : 35.0)
                        .codSurcharge(50.0)
                        .build());
            }
        }
        rateCardRepository.saveAll(rateCards);

        // 4. Create Users
        // Admin
        userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@logistics.com")
                .phone("9876543210")
                .role(Role.ROLE_ADMIN)
                .status("ACTIVE")
                .build());

        // Customers
        userRepository.save(User.builder()
                .username("customer1")
                .password(passwordEncoder.encode("customer123"))
                .email("customer1@gmail.com")
                .phone("9811111111")
                .role(Role.ROLE_CUSTOMER)
                .status("ACTIVE")
                .build());

        userRepository.save(User.builder()
                .username("customer2")
                .password(passwordEncoder.encode("customer123"))
                .email("customer2@gmail.com")
                .phone("9822222222")
                .role(Role.ROLE_CUSTOMER)
                .status("ACTIVE")
                .build());

        // Delivery Agents (Seeded with location coordinates near zones)
        // Agent North: Delhi (28.6139, 77.2090)
        userRepository.save(User.builder()
                .username("agent_north")
                .password(passwordEncoder.encode("agent123"))
                .email("agent.north@logistics.com")
                .phone("9833333333")
                .role(Role.ROLE_AGENT)
                .status("AVAILABLE")
                .latitude(28.6120)
                .longitude(77.2070)
                .build());

        // Agent West: Mumbai (18.9220, 72.8347)
        userRepository.save(User.builder()
                .username("agent_west")
                .password(passwordEncoder.encode("agent123"))
                .email("agent.west@logistics.com")
                .phone("9844444444")
                .role(Role.ROLE_AGENT)
                .status("AVAILABLE")
                .latitude(18.9210)
                .longitude(72.8330)
                .build());

        // Agent South: Bangalore (12.9716, 77.5946)
        userRepository.save(User.builder()
                .username("agent_south")
                .password(passwordEncoder.encode("agent123"))
                .email("agent.south@logistics.com")
                .phone("9855555555")
                .role(Role.ROLE_AGENT)
                .status("AVAILABLE")
                .latitude(12.9725)
                .longitude(77.5950)
                .build());

        System.out.println("Logistics data seeded successfully!");
    }
}
