package com.event_registration_platform.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Venue is required")
    private String venue;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDate;

    @Min(value = 1, message = "Capacity must be at least 1")
    private int maxCapacity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal price;
}