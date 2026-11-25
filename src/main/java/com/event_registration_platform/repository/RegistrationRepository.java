
package com.event_registration_platform.repository;

import com.event_registration_platform.entity.Registration;
import com.event_registration_platform.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    boolean existsByAttendeeIdAndEventIdAndStatus(
            Long attendeeId,
            Long eventId,
            RegistrationStatus status
    );

    long countByEventIdAndStatus(
            Long eventId,
            RegistrationStatus status
    );

    Optional<Registration> findByAttendeeIdAndEventIdAndStatus(
            Long attendeeId,
            Long eventId,
            RegistrationStatus status
    );


    Optional<Registration> findByAttendeeIdAndEventId(
            Long attendeeId,
            Long eventId
    );
}
