package com.event_registration_platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organiser_id", nullable = false)
    private User organiser;

    private String title;

    @Column(length = 2000)
    private String description;

    private String venue;

    private LocalDateTime eventDate;

    private int maxCapacity;

    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.DRAFT;

    private boolean featured = false;

    private boolean deleted = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}