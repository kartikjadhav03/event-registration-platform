
package com.event_registration_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_registrations",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"attendee_id", "event_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendee_id", nullable = false)
    private User attendee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    private LocalDateTime registeredAt;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus status = RegistrationStatus.REGISTERED;

    @PrePersist
    public void onCreate() {
        registeredAt = LocalDateTime.now();
    }
}
