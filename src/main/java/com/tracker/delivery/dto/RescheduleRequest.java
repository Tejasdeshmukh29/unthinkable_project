package com.tracker.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RescheduleRequest {
    @NotBlank
    private String scheduledDate; // ISO-8601 string or LocalDateTime string
}
