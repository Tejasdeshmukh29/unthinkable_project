package com.tracker.delivery.service;

import com.tracker.delivery.entity.Order;
import com.tracker.delivery.entity.Role;
import com.tracker.delivery.entity.User;
import com.tracker.delivery.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AgentAssignmentServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AgentAssignmentService agentAssignmentService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDistanceCalculation() {
        // Distance between Delhi (28.6139, 77.2090) and Mumbai (18.9220, 72.8347)
        // Should be roughly 1150-1200 km
        double distance = agentAssignmentService.calculateDistance(28.6139, 77.2090, 18.9220, 72.8347);
        assertEquals(1150.0, distance, 50.0); // Allow margin of error
    }

    @Test
    public void testAutoAssignmentToNearestAgent() {
        // Arrange
        // Order pickup in New Delhi pincode 110001 (coords approx: 28.6139, 77.2090)
        Order order = Order.builder()
                .pickupPincode("110001")
                .build();

        // 3 available agents:
        // agent1 is in Mumbai (far)
        User agentMumbai = User.builder()
                .id(1L)
                .username("agent_mumbai")
                .role(Role.ROLE_AGENT)
                .status("AVAILABLE")
                .latitude(18.9220)
                .longitude(72.8347)
                .build();

        // agent2 is in Delhi, but 5km away
        User agentDelhiFar = User.builder()
                .id(2L)
                .username("agent_delhi_far")
                .role(Role.ROLE_AGENT)
                .status("AVAILABLE")
                .latitude(28.6500)
                .longitude(77.2500)
                .build();

        // agent3 is in Delhi, very close (0.2km)
        User agentDelhiClose = User.builder()
                .id(3L)
                .username("agent_delhi_close")
                .role(Role.ROLE_AGENT)
                .status("AVAILABLE")
                .latitude(28.6130)
                .longitude(77.2085)
                .build();

        when(userRepository.findByRoleAndStatus(Role.ROLE_AGENT, "AVAILABLE"))
                .thenReturn(List.of(agentMumbai, agentDelhiFar, agentDelhiClose));

        // Act
        User assignedAgent = agentAssignmentService.findNearestAvailableAgent(order);

        // Assert
        assertNotNull(assignedAgent);
        assertEquals(3L, assignedAgent.getId());
        assertEquals("agent_delhi_close", assignedAgent.getUsername());
    }

    @Test
    public void testAutoAssignmentNoAgentsAvailable() {
        Order order = Order.builder().pickupPincode("110001").build();
        when(userRepository.findByRoleAndStatus(Role.ROLE_AGENT, "AVAILABLE")).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> {
            agentAssignmentService.findNearestAvailableAgent(order);
        });
    }
}
