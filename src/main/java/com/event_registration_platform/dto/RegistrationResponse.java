package com.event_registration_platform.dto;

import com.event_registration_platform.entity.RegistrationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RegistrationResponse {

    private Long registrationId;
    private Long eventId;
    private String eventTitle;
    private String attendeeName;
    private LocalDateTime registeredAt;
    private RegistrationStatus status;
}