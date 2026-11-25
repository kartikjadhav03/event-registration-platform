
package com.event_registration_platform;

import com.event_registration_platform.dto.RegistrationResponse;
import com.event_registration_platform.entity.*;
import com.event_registration_platform.repository.EventRepository;
import com.event_registration_platform.repository.RegistrationRepository;
import com.event_registration_platform.repository.UserRepository;
import com.event_registration_platform.service.RegistrationServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegistrationServiceImplTest {

    private RegistrationRepository registrationRepository;
    private EventRepository eventRepository;
    private UserRepository userRepository;

    private RegistrationServiceImpl registrationService;

    private final String attendeeEmail = "attendee@test.com";
    private final Long eventId = 4L;

    @BeforeEach
    void setUp() {

        registrationRepository = mock(RegistrationRepository.class);
        eventRepository = mock(EventRepository.class);
        userRepository = mock(UserRepository.class);

        registrationService = new RegistrationServiceImpl(
                registrationRepository,
                eventRepository,
                userRepository
        );
    }

    private User createAttendee() {

        User user = new User();
        user.setId(1L);
        user.setName("Test Attendee");
        user.setEmail(attendeeEmail);
        user.setRole(Role.ATTENDEE);

        return user;
    }

    private Event createEvent() {

        Event event = new Event();
        event.setId(eventId);
        event.setTitle("Registration Testing Event");
        event.setMaxCapacity(2);
        event.setStatus(EventStatus.PUBLISHED);

        return event;
    }

    private Registration createRegistration(
            User attendee,
            Event event,
            RegistrationStatus status
    ) {

        Registration registration = new Registration();
        registration.setId(10L);
        registration.setAttendee(attendee);
        registration.setEvent(event);
        registration.setStatus(status);

        return registration;
    }

    // 1. Successful registration

    @Test
    void shouldRegisterAttendeeWhenSeatsAreAvailable() {

        User attendee = createAttendee();
        Event event = createEvent();

        when(userRepository.findByEmail(attendeeEmail))
                .thenReturn(Optional.of(attendee));

        when(eventRepository.findByIdAndDeletedFalseWithLock(eventId))
                .thenReturn(Optional.of(event));

        when(registrationRepository.findByAttendeeIdAndEventId(1L, eventId))
                .thenReturn(Optional.empty());

        when(registrationRepository.countByEventIdAndStatus(
                eventId, RegistrationStatus.REGISTERED))
                .thenReturn(1L);

        when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(invocation -> {
                    Registration registration = invocation.getArgument(0);
                    registration.setId(10L);
                    return registration;
                });

        RegistrationResponse response =
                registrationService.registerForEvent(eventId, attendeeEmail);

        assertEquals(10L, response.getRegistrationId());
        assertEquals(eventId, response.getEventId());
        assertEquals("Test Attendee", response.getAttendeeName());
        assertEquals(RegistrationStatus.REGISTERED, response.getStatus());

        verify(registrationRepository, times(1))
                .save(any(Registration.class));
    }

    // 2. Reject registration when event is full

    @Test
    void shouldRejectRegistrationWhenEventIsFull() {

        User attendee = createAttendee();
        Event event = createEvent();

        when(userRepository.findByEmail(attendeeEmail))
                .thenReturn(Optional.of(attendee));

        when(eventRepository.findByIdAndDeletedFalseWithLock(eventId))
                .thenReturn(Optional.of(event));

        when(registrationRepository.findByAttendeeIdAndEventId(1L, eventId))
                .thenReturn(Optional.empty());

        when(registrationRepository.countByEventIdAndStatus(
                eventId, RegistrationStatus.REGISTERED))
                .thenReturn(2L);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.registerForEvent(eventId, attendeeEmail)
        );

        assertEquals("Event is fully booked", exception.getMessage());

        verify(registrationRepository, never())
                .save(any(Registration.class));
    }

    // 3. Reject duplicate active registration

    @Test
    void shouldRejectDuplicateRegistration() {

        User attendee = createAttendee();
        Event event = createEvent();

        Registration existing = createRegistration(
                attendee, event, RegistrationStatus.REGISTERED
        );

        when(userRepository.findByEmail(attendeeEmail))
                .thenReturn(Optional.of(attendee));

        when(eventRepository.findByIdAndDeletedFalseWithLock(eventId))
                .thenReturn(Optional.of(event));

        when(registrationRepository.findByAttendeeIdAndEventId(1L, eventId))
                .thenReturn(Optional.of(existing));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.registerForEvent(eventId, attendeeEmail)
        );

        assertEquals("Already registered for this event", exception.getMessage());

        verify(registrationRepository, never())
                .save(any(Registration.class));
    }

    // 4. Reject non-attendee user

    @Test
    void shouldRejectRegistrationForNonAttendeeUser() {

        User user = createAttendee();

        Role nonAttendeeRole = Arrays.stream(Role.values())
                .filter(role -> role != Role.ATTENDEE)
                .findFirst()
                .orElseThrow();

        user.setRole(nonAttendeeRole);

        when(userRepository.findByEmail(attendeeEmail))
                .thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.registerForEvent(eventId, attendeeEmail)
        );

        assertEquals(
                "Only attendees can register for events",
                exception.getMessage()
        );

        verify(eventRepository, never())
                .findByIdAndDeletedFalseWithLock(any());

        verify(registrationRepository, never())
                .save(any(Registration.class));
    }

    // 5. Reject registration for unpublished event

    @Test
    void shouldRejectRegistrationWhenEventIsNotPublished() {

        User attendee = createAttendee();
        Event event = createEvent();

        event.setStatus(EventStatus.DRAFT);

        when(userRepository.findByEmail(attendeeEmail))
                .thenReturn(Optional.of(attendee));

        when(eventRepository.findByIdAndDeletedFalseWithLock(eventId))
                .thenReturn(Optional.of(event));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.registerForEvent(eventId, attendeeEmail)
        );

        assertEquals(
                "Only published events can be registered",
                exception.getMessage()
        );

        verify(registrationRepository, never())
                .save(any(Registration.class));
    }

    // 6. Reactivate cancelled registration

    @Test
    void shouldReactivateCancelledRegistration() {

        User attendee = createAttendee();
        Event event = createEvent();

        Registration existing = createRegistration(
                attendee, event, RegistrationStatus.CANCELLED
        );

        when(userRepository.findByEmail(attendeeEmail))
                .thenReturn(Optional.of(attendee));

        when(eventRepository.findByIdAndDeletedFalseWithLock(eventId))
                .thenReturn(Optional.of(event));

        when(registrationRepository.findByAttendeeIdAndEventId(1L, eventId))
                .thenReturn(Optional.of(existing));

        when(registrationRepository.countByEventIdAndStatus(
                eventId, RegistrationStatus.REGISTERED))
                .thenReturn(1L);

        when(registrationRepository.save(existing))
                .thenReturn(existing);

        RegistrationResponse response =
                registrationService.registerForEvent(eventId, attendeeEmail);

        assertEquals(RegistrationStatus.REGISTERED, response.getStatus());

        verify(registrationRepository, times(1)).save(existing);
    }

    // 7. Reject reactivation when event is full

    @Test
    void shouldRejectReactivationWhenEventIsFull() {

        User attendee = createAttendee();
        Event event = createEvent();

        Registration existing = createRegistration(
                attendee, event, RegistrationStatus.CANCELLED
        );

        when(userRepository.findByEmail(attendeeEmail))
                .thenReturn(Optional.of(attendee));

        when(eventRepository.findByIdAndDeletedFalseWithLock(eventId))
                .thenReturn(Optional.of(event));

        when(registrationRepository.findByAttendeeIdAndEventId(1L, eventId))
                .thenReturn(Optional.of(existing));

        when(registrationRepository.countByEventIdAndStatus(
                eventId, RegistrationStatus.REGISTERED))
                .thenReturn(2L);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.registerForEvent(eventId, attendeeEmail)
        );

        assertEquals("Event is fully booked", exception.getMessage());

        verify(registrationRepository, never())
                .save(any(Registration.class));
    }

    // 8. Successful cancellation

    @Test
    void shouldCancelActiveRegistration() {

        User attendee = createAttendee();
        Event event = createEvent();

        Registration registration = createRegistration(
                attendee, event, RegistrationStatus.REGISTERED
        );

        when(userRepository.findByEmail(attendeeEmail))
                .thenReturn(Optional.of(attendee));

        when(eventRepository.findByIdAndDeletedFalseWithLock(eventId))
                .thenReturn(Optional.of(event));

        when(registrationRepository.findByAttendeeIdAndEventIdAndStatus(
                1L, eventId, RegistrationStatus.REGISTERED))
                .thenReturn(Optional.of(registration));

        when(registrationRepository.save(registration))
                .thenReturn(registration);

        RegistrationResponse response =
                registrationService.cancelRegistration(eventId, attendeeEmail);

        assertEquals(RegistrationStatus.CANCELLED, response.getStatus());

        verify(registrationRepository, times(1)).save(registration);
    }

    // 9. Reject cancellation without active registration

    @Test
    void shouldRejectCancellationWhenNoActiveRegistrationExists() {

        User attendee = createAttendee();
        Event event = createEvent();

        when(userRepository.findByEmail(attendeeEmail))
                .thenReturn(Optional.of(attendee));

        when(eventRepository.findByIdAndDeletedFalseWithLock(eventId))
                .thenReturn(Optional.of(event));

        when(registrationRepository.findByAttendeeIdAndEventIdAndStatus(
                1L, eventId, RegistrationStatus.REGISTERED))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.cancelRegistration(eventId, attendeeEmail)
        );

        assertEquals("Active registration not found", exception.getMessage());

        verify(registrationRepository, never())
                .save(any(Registration.class));
    }

    // 10. Reject registration when user does not exist

    @Test
    void shouldRejectRegistrationWhenUserDoesNotExist() {

        when(userRepository.findByEmail(attendeeEmail))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.registerForEvent(eventId, attendeeEmail)
        );

        assertEquals("User not found", exception.getMessage());

        verify(registrationRepository, never())
                .save(any(Registration.class));
    }

    // 11. Reject registration when event does not exist

    @Test
    void shouldRejectRegistrationWhenEventDoesNotExist() {

        User attendee = createAttendee();

        when(userRepository.findByEmail(attendeeEmail))
                .thenReturn(Optional.of(attendee));

        when(eventRepository.findByIdAndDeletedFalseWithLock(eventId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.registerForEvent(eventId, attendeeEmail)
        );

        assertEquals("Event not found", exception.getMessage());

        verify(registrationRepository, never())
                .save(any(Registration.class));
    }
}
