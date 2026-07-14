package com.tracker.delivery.service;

import com.tracker.delivery.entity.Notification;
import com.tracker.delivery.entity.Order;
import com.tracker.delivery.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void sendEmail(String recipientEmail, String subject, String message) {
        Notification notification = Notification.builder()
                .recipientEmail(recipientEmail)
                .channel("EMAIL")
                .subject(subject)
                .message(message)
                .status("SENT")
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        // Simulated Logging
        System.out.println("=================================================");
        System.out.println("SIMULATED EMAIL SENT");
        System.out.println("To: " + recipientEmail);
        System.out.println("Subject: " + subject);
        System.out.println("Message: " + message);
        System.out.println("=================================================");
    }

    public void sendSms(String recipientPhone, String message) {
        Notification notification = Notification.builder()
                .recipientPhone(recipientPhone)
                .channel("SMS")
                .message(message)
                .status("SENT")
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        // Simulated Logging
        System.out.println("=================================================");
        System.out.println("SIMULATED SMS SENT");
        System.out.println("To: " + recipientPhone);
        System.out.println("Message: " + message);
        System.out.println("=================================================");
    }

    public void notifyStatusChange(Order order, String notes) {
        String customerEmail = order.getCustomer().getEmail();
        String customerPhone = order.getCustomer().getPhone();
        String orderNo = order.getOrderNumber();
        String status = order.getStatus();

        String subject = "Update on your Order " + orderNo;
        String emailMessage = "";
        String smsMessage = "";

        switch (status) {
            case "CREATED" -> {
                emailMessage = String.format("Dear %s, your order %s has been created successfully. Estimated delivery charge is Rs. %.2f. Thank you!",
                        order.getCustomer().getUsername(), orderNo, order.getTotalCharge());
                smsMessage = String.format("Order %s created. Charge: Rs. %.2f. Track online.", orderNo, order.getTotalCharge());
            }
            case "ASSIGNED" -> {
                String agentName = order.getAgent() != null ? order.getAgent().getUsername() : "a delivery partner";
                String agentPhone = order.getAgent() != null ? order.getAgent().getPhone() : "N/A";
                emailMessage = String.format("Dear Customer, delivery agent %s (%s) has been assigned to your order %s.",
                        agentName, agentPhone, orderNo);
                smsMessage = String.format("Agent %s (%s) assigned to your order %s.", agentName, agentPhone, orderNo);
            }
            case "PICKED_UP" -> {
                emailMessage = String.format("Dear Customer, your order %s has been picked up by our delivery agent and is moving to our hub.", orderNo);
                smsMessage = String.format("Order %s picked up. Track live status in your dashboard.", orderNo);
            }
            case "IN_TRANSIT" -> {
                emailMessage = String.format("Dear Customer, your order %s is currently in transit between sorting hubs.", orderNo);
                smsMessage = String.format("Order %s is in transit.", orderNo);
            }
            case "OUT_FOR_DELIVERY" -> {
                emailMessage = String.format("Dear Customer, your order %s is out for delivery! Our agent will contact you shortly.", orderNo);
                smsMessage = String.format("Order %s is out for delivery today. Keep your phone handy.", orderNo);
            }
            case "DELIVERED" -> {
                emailMessage = String.format("Dear Customer, your order %s has been successfully delivered. Thank you for shipping with us!", orderNo);
                smsMessage = String.format("Order %s delivered. Hope you enjoyed our service!", orderNo);
            }
            case "FAILED" -> {
                emailMessage = String.format("Dear Customer, delivery attempt for order %s failed. Reason: %s. Please log in to reschedule your delivery for a new date.",
                        orderNo, (notes != null ? notes : "No response / address locked"));
                smsMessage = String.format("Delivery failed for order %s. Reason: %s. Reschedule via customer portal.",
                        orderNo, (notes != null ? notes : "Not available"));
            }
            default -> {
                emailMessage = String.format("Dear Customer, status of order %s has changed to %s.", orderNo, status);
                smsMessage = String.format("Order %s status: %s.", orderNo, status);
            }
        }

        sendEmail(customerEmail, subject, emailMessage);
        sendSms(customerPhone, smsMessage);
    }
}
