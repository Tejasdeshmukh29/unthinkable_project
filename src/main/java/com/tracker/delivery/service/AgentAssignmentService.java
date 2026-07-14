package com.tracker.delivery.service;

import com.tracker.delivery.entity.Order;
import com.tracker.delivery.entity.Role;
import com.tracker.delivery.entity.User;
import com.tracker.delivery.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AgentAssignmentService {

    private final UserRepository userRepository;

    public AgentAssignmentService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Resolves coordinates based on common pincodes for mock tracking accuracy
    public double[] getPincodeCoordinates(String pincode) {
        if (pincode == null) {
            return new double[]{0.0, 0.0};
        }
        return switch (pincode.trim()) {
            // Zone North: New Delhi
            case "110001" -> new double[]{28.6139, 77.2090};
            case "110002" -> new double[]{28.6250, 77.2150};
            // Zone West: Mumbai
            case "400001" -> new double[]{18.9220, 72.8347};
            case "400002" -> new double[]{18.9400, 72.8250};
            // Zone South: Bangalore/Chennai
            case "560001" -> new double[]{12.9716, 77.5946};
            case "600001" -> new double[]{13.0827, 80.2707};
            // Zone East: Kolkata
            case "700001" -> new double[]{22.5726, 88.3639};
            // Fallback default coordinates
            default -> new double[]{20.5937, 78.9629}; // Center of India
        };
    }

    // Haversine formula to compute distance in Kilometers
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public User findNearestAvailableAgent(Order order) {
        List<User> availableAgents = userRepository.findByRoleAndStatus(Role.ROLE_AGENT, "AVAILABLE");
        if (availableAgents.isEmpty()) {
            throw new IllegalStateException("No available delivery agents found in the system.");
        }

        double[] pickupCoords = getPincodeCoordinates(order.getPickupPincode());
        double pickupLat = pickupCoords[0];
        double pickupLon = pickupCoords[1];

        User nearestAgent = null;
        double minDistance = Double.MAX_VALUE;

        for (User agent : availableAgents) {
            double agentLat = agent.getLatitude() != null ? agent.getLatitude() : 0.0;
            double agentLon = agent.getLongitude() != null ? agent.getLongitude() : 0.0;

            // Compute distance
            double distance = calculateDistance(pickupLat, pickupLon, agentLat, agentLon);
            if (distance < minDistance) {
                minDistance = distance;
                nearestAgent = agent;
            }
        }

        return nearestAgent;
    }
}
