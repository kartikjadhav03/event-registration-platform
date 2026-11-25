package com.event_registration_platform.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;

    private String title;

    private String description;

    private String venue;

    private LocalDateTime eventDate;

    private int maxCapacity;

    private BigDecimal price;

    private String status;

    private boolean featured;

    private String organiserName;
}