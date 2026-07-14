package com.tracker.delivery.service;

import com.tracker.delivery.entity.*;
import com.tracker.delivery.repository.OrderRepository;
import com.tracker.delivery.repository.OrderTrackingHistoryRepository;
import com.tracker.delivery.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderTrackingHistoryRepository trackingRepository;
    private final UserRepository userRepository;
    private final RateCalculationService rateCalculationService;
    private final AgentAssignmentService agentAssignmentService;
    private final NotificationService notificationService;

    public OrderService(
            OrderRepository orderRepository,
            OrderTrackingHistoryRepository trackingRepository,
            UserRepository userRepository,
            RateCalculationService rateCalculationService,
            AgentAssignmentService agentAssignmentService,
            NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.trackingRepository = trackingRepository;
        this.userRepository = userRepository;
        this.rateCalculationService = rateCalculationService;
        this.agentAssignmentService = agentAssignmentService;
        this.notificationService = notificationService;
    }

    public Order createOrder(
            User customer, String pickupAddress, String pickupPincode,
            String dropAddress, String dropPincode,
            Double lengthCm, Double widthCm, Double heightCm,
            Double actualWeightKg, String orderType, String paymentType,
            User actor) {

        // Calculate pricing
        RateCalculationService.RateCalculationResult pricing = rateCalculationService.calculate(
                lengthCm, widthCm, heightCm, actualWeightKg,
                pickupPincode, dropPincode, orderType, paymentType);

        // Generate clean order tracking number
        String orderNumber = "LMD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customer(customer)
                .pickupAddress(pickupAddress)
                .pickupPincode(pickupPincode)
                .dropAddress(dropAddress)
                .dropPincode(dropPincode)
                .pickupZone(pricing.pickupZone)
                .dropZone(pricing.dropZone)
                .lengthCm(lengthCm)
                .widthCm(widthCm)
                .heightCm(heightCm)
                .actualWeightKg(actualWeightKg)
                .chargeableWeightKg(pricing.chargeableWeight)
                .orderType(orderType)
                .paymentType(paymentType)
                .deliveryCharge(pricing.deliveryCharge)
                .codSurcharge(pricing.codSurcharge)
                .totalCharge(pricing.totalCharge)
                .status("CREATED")
                .scheduledDate(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        // Log history entry
        logTracking(savedOrder, "CREATED", actor, "Order placed successfully.");

        // Send notifications
        notificationService.notifyStatusChange(savedOrder, null);

        return savedOrder;
    }

    public Order assignAgentManually(Long orderId, Long agentId, User actor) {
        Order order = getOrderById(orderId);
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));

        if (agent.getRole() != Role.ROLE_AGENT) {
            throw new IllegalArgumentException("Assigned user is not a delivery agent.");
        }

        // Free previous agent if any
        if (order.getAgent() != null) {
            User oldAgent = order.getAgent();
            oldAgent.setStatus("AVAILABLE");
            userRepository.save(oldAgent);
        }

        // Set agent and status
        order.setAgent(agent);
        order.setStatus("ASSIGNED");
        orderRepository.save(order);

        // Update agent status
        agent.setStatus("BUSY");
        userRepository.save(agent);

        logTracking(order, "ASSIGNED", actor, "Agent " + agent.getUsername() + " manually assigned.");

        notificationService.notifyStatusChange(order, null);

        return order;
    }

    public Order assignAgentAutomatically(Long orderId, User actor) {
        Order order = getOrderById(orderId);

        // Find nearest available agent
        User agent = agentAssignmentService.findNearestAvailableAgent(order);

        // Free previous agent if any
        if (order.getAgent() != null) {
            User oldAgent = order.getAgent();
            oldAgent.setStatus("AVAILABLE");
            userRepository.save(oldAgent);
        }

        // Set agent and status
        order.setAgent(agent);
        order.setStatus("ASSIGNED");
        orderRepository.save(order);

        // Update agent status
        agent.setStatus("BUSY");
        userRepository.save(agent);

        logTracking(order, "ASSIGNED", actor, "Agent " + agent.getUsername() + " automatically assigned (nearest).");

        notificationService.notifyStatusChange(order, null);

        return order;
    }

    public Order updateOrderStatus(Long orderId, String newStatus, String notes, User actor) {
        Order order = getOrderById(orderId);
        String oldStatus = order.getStatus();

        // If actor is ADMIN, we allow overriding any status.
        // Otherwise, enforce valid delivery agent transition.
        if (actor.getRole() != Role.ROLE_ADMIN) {
            validateAgentTransition(oldStatus, newStatus);
            // Verify agent is the assigned one
            if (order.getAgent() == null || !order.getAgent().getId().equals(actor.getId())) {
                throw new IllegalStateException("You are not authorized to update status for this order.");
            }
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        // If status becomes DELIVERED or FAILED, free the assigned agent
        if (("DELIVERED".equals(newStatus) || "FAILED".equals(newStatus)) && order.getAgent() != null) {
            User agent = order.getAgent();
            agent.setStatus("AVAILABLE");
            userRepository.save(agent);
        }

        logTracking(updatedOrder, newStatus, actor, notes);

        notificationService.notifyStatusChange(updatedOrder, notes);

        return updatedOrder;
    }

    public Order rescheduleOrder(Long orderId, LocalDateTime newDate, User actor) {
        Order order = getOrderById(orderId);

        if (!"FAILED".equals(order.getStatus())) {
            throw new IllegalStateException("Only failed orders can be rescheduled.");
        }

        // Release current agent (should already be AVAILABLE, but update database)
        if (order.getAgent() != null) {
            User agent = order.getAgent();
            agent.setStatus("AVAILABLE");
            userRepository.save(agent);
            order.setAgent(null); // Clear agent so they can be reassigned
        }

        order.setStatus("CREATED"); // Set back to CREATED for re-assignment
        order.setScheduledDate(newDate);
        Order updatedOrder = orderRepository.save(order);

        logTracking(updatedOrder, "CREATED", actor, "Rescheduled for " + newDate + ". Awaiting agent reassignment.");

        notificationService.notifyStatusChange(updatedOrder, "Rescheduled");

        return updatedOrder;
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));
    }

    public Optional<Order> getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public List<Order> getCustomerOrders(User customer) {
        return orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public List<Order> getAgentOrders(User agent) {
        return orderRepository.findByAgentOrderByCreatedAtDesc(agent);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Order> getFilteredOrders(String status, Long zoneId, Long agentId) {
        // Convert empty values to null
        String statusFilter = (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) ? status : null;
        return orderRepository.findFilteredOrders(statusFilter, zoneId, agentId);
    }

    public List<OrderTrackingHistory> getTrackingHistory(Long orderId) {
        return trackingRepository.findByOrderIdOrderByTimestampAsc(orderId);
    }

    private void logTracking(Order order, String status, User actor, String notes) {
        OrderTrackingHistory history = OrderTrackingHistory.builder()
                .order(order)
                .status(status)
                .actor(actor)
                .actorRole(actor != null ? actor.getRole().name() : "SYSTEM")
                .notes(notes)
                .build();
        trackingRepository.save(history);
    }

    private void validateAgentTransition(String oldStatus, String newStatus) {
        boolean valid = switch (oldStatus) {
            case "ASSIGNED" -> "PICKED_UP".equals(newStatus) || "FAILED".equals(newStatus);
            case "PICKED_UP" -> "IN_TRANSIT".equals(newStatus) || "FAILED".equals(newStatus);
            case "IN_TRANSIT" -> "OUT_FOR_DELIVERY".equals(newStatus) || "FAILED".equals(newStatus);
            case "OUT_FOR_DELIVERY" -> "DELIVERED".equals(newStatus) || "FAILED".equals(newStatus);
            default -> false; // CREATED requires assignment (admin action); FAILED / DELIVERED are terminal states
        };

        if (!valid) {
            throw new IllegalStateException(String.format("Invalid status transition from %s to %s for delivery agent.", oldStatus, newStatus));
        }
    }
}
