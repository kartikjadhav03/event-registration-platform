
package com.event_registration_platform.controller;

import com.event_registration_platform.dto.RegistrationResponse;
import com.event_registration_platform.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/{eventId}/register")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<RegistrationResponse> registerForEvent(
            @PathVariable Long eventId,
            Authentication authentication) {

        RegistrationResponse response = registrationService.registerForEvent(
                eventId, authentication.getName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/cancel-registration")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<RegistrationResponse> cancelRegistration(
            @PathVariable Long eventId,
            Authentication authentication) {

        RegistrationResponse response = registrationService.cancelRegistration(
                eventId, authentication.getName());

        return ResponseEntity.ok(response);
    }
}
