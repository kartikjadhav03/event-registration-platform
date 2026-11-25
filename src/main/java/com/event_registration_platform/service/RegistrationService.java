package com.event_registration_platform.service;

import com.event_registration_platform.dto.RegistrationResponse;

public interface RegistrationService {

    RegistrationResponse registerForEvent(Long eventId, String attendeeEmail);

    RegistrationResponse cancelRegistration(Long eventId, String attendeeEmail);
}