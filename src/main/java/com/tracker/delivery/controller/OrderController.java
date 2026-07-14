package com.tracker.delivery.controller;

import com.tracker.delivery.dto.OrderRequest;
import com.tracker.delivery.dto.RescheduleRequest;
import com.tracker.delivery.dto.StatusUpdateRequest;
import com.tracker.delivery.entity.*;
import com.tracker.delivery.service.OrderService;
import com.tracker.delivery.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest request, Principal principal) {
        try {
            User actor = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            User customer;
            if (actor.getRole() == Role.ROLE_ADMIN) {
                if (request.getCustomerId() == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Admin must specify customerId"));
                }
                customer = userService.getAllCustomers().stream()
                        .filter(c -> c.getId().equals(request.getCustomerId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + request.getCustomerId()));
            } else if (actor.getRole() == Role.ROLE_CUSTOMER) {
                customer = actor;
            } else {
                return ResponseEntity.status(403).body(Map.of("error", "Agents cannot place orders"));
            }

            Order order = orderService.createOrder(
                    customer,
                    request.getPickupAddress(),
                    request.getPickupPincode(),
                    request.getDropAddress(),
                    request.getDropPincode(),
                    request.getLengthCm(),
                    request.getWidthCm(),
                    request.getHeightCm(),
                    request.getActualWeightKg(),
                    request.getOrderType(),
                    request.getPaymentType(),
                    actor
            );

            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long agentId,
            Principal principal) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() == Role.ROLE_ADMIN) {
            // Admin can search and filter
            List<Order> orders = orderService.getFilteredOrders(status, zoneId, agentId);
            return ResponseEntity.ok(orders);
        } else if (user.getRole() == Role.ROLE_CUSTOMER) {
            List<Order> orders = orderService.getCustomerOrders(user);
            return ResponseEntity.ok(orders);
        } else {
            // Agent gets assigned orders
            List<Order> orders = orderService.getAgentOrders(user);
            return ResponseEntity.ok(orders);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id, Principal principal) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Order order = orderService.getOrderById(id);

        // Security check
        if (user.getRole() == Role.ROLE_CUSTOMER && !order.getCustomer().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        if (user.getRole() == Role.ROLE_AGENT && (order.getAgent() == null || !order.getAgent().getId().equals(user.getId()))) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        return ResponseEntity.ok(order);
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<?> getOrderTracking(@PathVariable Long id) {
        List<OrderTrackingHistory> history = orderService.getTrackingHistory(id);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/assign-auto")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignAgentAuto(@PathVariable Long id, Principal principal) {
        try {
            User actor = userService.findByUsername(principal.getName()).orElse(null);
            Order order = orderService.assignAgentAutomatically(id, actor);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/assign-manual/{agentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignAgentManual(
            @PathVariable Long id,
            @PathVariable Long agentId,
            Principal principal) {
        try {
            User actor = userService.findByUsername(principal.getName()).orElse(null);
            Order order = orderService.assignAgentManually(id, agentId, actor);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            Principal principal) {
        try {
            User actor = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Order order = orderService.updateOrderStatus(id, request.getStatus(), request.getNotes(), actor);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<?> rescheduleOrder(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleRequest request,
            Principal principal) {
        try {
            User actor = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Order order = orderService.getOrderById(id);

            // Access check: Admin or customer owning the order
            if (actor.getRole() != Role.ROLE_ADMIN && !order.getCustomer().getId().equals(actor.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            // Parse reschedule date
            LocalDateTime newDate;
            try {
                // Support multiple ISO formats or custom
                newDate = LocalDateTime.parse(request.getScheduledDate(), DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e) {
                // Fallback to parsing YYYY-MM-DD
                newDate = LocalDateTime.parse(request.getScheduledDate() + "T10:00:00", DateTimeFormatter.ISO_DATE_TIME);
            }

            Order updatedOrder = orderService.rescheduleOrder(id, newDate, actor);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
