
package com.event_registration_platform.service;

import com.event_registration_platform.dto.RegistrationResponse;
import com.event_registration_platform.entity.*;
import com.event_registration_platform.repository.EventRepository;
import com.event_registration_platform.repository.RegistrationRepository;
import com.event_registration_platform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public RegistrationServiceImpl(
            RegistrationRepository registrationRepository,
            EventRepository eventRepository,
            UserRepository userRepository) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public RegistrationResponse registerForEvent(Long eventId, String attendeeEmail) {

        User attendee = userRepository.findByEmail(attendeeEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (attendee.getRole() != Role.ATTENDEE) {
            throw new RuntimeException("Only attendees can register for events");
        }

        Event event = eventRepository.findByIdAndDeletedFalseWithLock(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new RuntimeException("Only published events can be registered");
        }

        // Find existing registration
        var existingRegistration = registrationRepository
                .findByAttendeeIdAndEventId(attendee.getId(), eventId);

        if (existingRegistration.isPresent()) {

            Registration registration = existingRegistration.get();

            // Prevent duplicate active registration
            if (registration.getStatus() == RegistrationStatus.REGISTERED) {
                throw new RuntimeException("Already registered for this event");
            }

            // Check event capacity
            long registeredCount = registrationRepository.countByEventIdAndStatus(
                    eventId, RegistrationStatus.REGISTERED);

            if (registeredCount >= event.getMaxCapacity()) {
                throw new RuntimeException("Event is fully booked");
            }

            // Reactivate cancelled registration
            registration.setStatus(RegistrationStatus.REGISTERED);

            return toResponse(registrationRepository.save(registration));
        }

        // Check capacity for new registration
        long registeredCount = registrationRepository.countByEventIdAndStatus(
                eventId, RegistrationStatus.REGISTERED);

        if (registeredCount >= event.getMaxCapacity()) {
            throw new RuntimeException("Event is fully booked");
        }

        // Create new registrdation
        Registration registration = new Registration();
        registration.setAttendee(attendee);
        registration.setEvent(event);
        registration.setStatus(RegistrationStatus.REGISTERED);

        return toResponse(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public RegistrationResponse cancelRegistration(Long eventId, String attendeeEmail) {

        User attendee = userRepository.findByEmail(attendeeEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        eventRepository.findByIdAndDeletedFalseWithLock(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Registration registration = registrationRepository
                .findByAttendeeIdAndEventIdAndStatus(
                        attendee.getId(),
                        eventId,
                        RegistrationStatus.REGISTERED)
                .orElseThrow(() -> new RuntimeException("Active registration not found"));

        registration.setStatus(RegistrationStatus.CANCELLED);

        return toResponse(registrationRepository.save(registration));
    }

    private RegistrationResponse toResponse(Registration registration) {

        RegistrationResponse response = new RegistrationResponse();

        response.setRegistrationId(registration.getId());
        response.setEventId(registration.getEvent().getId());
        response.setEventTitle(registration.getEvent().getTitle());
        response.setAttendeeName(registration.getAttendee().getName());
        response.setRegisteredAt(registration.getRegisteredAt());
        response.setStatus(registration.getStatus());

        return response;
    }
}
