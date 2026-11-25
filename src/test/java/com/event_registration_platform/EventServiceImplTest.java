
package com.event_registration_platform;

import com.event_registration_platform.dto.EventRequest;
import com.event_registration_platform.dto.EventResponse;
import com.event_registration_platform.entity.*;
import com.event_registration_platform.repository.EventRepository;
import com.event_registration_platform.repository.UserRepository;
import com.event_registration_platform.service.EventServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventServiceImplTest {

    private EventRepository eventRepository;
    private UserRepository userRepository;
    private EventServiceImpl eventService;

    private final Long eventId = 4L;
    private final String organiserEmail = "organiser@test.com";

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        userRepository = mock(UserRepository.class);

        eventService = new EventServiceImpl(
                eventRepository,
                userRepository
        );
    }

    private User createOrganiser() {
        User user = new User();
        user.setId(1L);
        user.setName("Test Organiser");
        user.setEmail(organiserEmail);
        user.setRole(Role.ORGANISER);
        return user;
    }

    private Event createEvent(User organiser) {
        Event event = new Event();
        event.setId(eventId);
        event.setOrganiser(organiser);
        event.setTitle("Java Backend Workshop");
        event.setDescription("Spring Boot workshop");
        event.setVenue("Pune");
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setMaxCapacity(50);
        event.setPrice(new BigDecimal("499.00"));
        event.setStatus(EventStatus.DRAFT);
        event.setFeatured(false);
        event.setDeleted(false);
        return event;
    }

    private EventRequest createRequest() {
        EventRequest request = new EventRequest();
        request.setTitle("Updated Java Workshop");
        request.setDescription("Updated description");
        request.setVenue("Mumbai");
        request.setEventDate(LocalDateTime.now().plusDays(15));
        request.setMaxCapacity(100);
        request.setPrice(new BigDecimal("599.00"));
        return request;
    }

    // 1. Create event successfully

    @Test
    void shouldCreateEventSuccessfully() {
        User organiser = createOrganiser();
        EventRequest request = createRequest();

        when(userRepository.findByEmail(organiserEmail))
                .thenReturn(Optional.of(organiser));

        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> {
                    Event event = invocation.getArgument(0);
                    event.setId(eventId);
                    return event;
                });

        EventResponse response =
                eventService.createEvent(request, organiserEmail);

        assertEquals(eventId, response.getId());
        assertEquals("Updated Java Workshop", response.getTitle());
        assertEquals("Mumbai", response.getVenue());
        assertEquals("Test Organiser", response.getOrganiserName());

        verify(eventRepository).save(any(Event.class));
    }

    // 2. Reject creation when organiser does not exist

    @Test
    void shouldRejectCreationWhenOrganiserDoesNotExist() {
        when(userRepository.findByEmail(organiserEmail))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventService.createEvent(createRequest(), organiserEmail)
        );

        assertEquals("User not found", exception.getMessage());

        verify(eventRepository, never()).save(any(Event.class));
    }

    // 3. Get published event successfully

    @Test
    void shouldGetPublishedEventSuccessfully() {
        User organiser = createOrganiser();
        Event event = createEvent(organiser);
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        EventResponse response = eventService.getEvent(eventId);

        assertEquals(eventId, response.getId());
        assertEquals("Java Backend Workshop", response.getTitle());
        assertEquals("PUBLISHED", response.getStatus());
    }

    // 4. Reject unpublished event

    @Test
    void shouldRejectGettingDraftEvent() {
        Event event = createEvent(createOrganiser());

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventService.getEvent(eventId)
        );

        assertEquals("Published event not found", exception.getMessage());
    }

    // 5. Get published events with pagination

    @Test
    void shouldGetAllPublishedEvents() {
        User organiser = createOrganiser();

        Event event1 = createEvent(organiser);
        event1.setStatus(EventStatus.PUBLISHED);

        Event event2 = createEvent(organiser);
        event2.setId(5L);
        event2.setTitle("Spring Boot Seminar");
        event2.setStatus(EventStatus.PUBLISHED);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Event> eventPage = new PageImpl<>(
                List.of(event1, event2),
                pageable,
                2
        );

        when(eventRepository.findByDeletedFalseAndStatus(
                EventStatus.PUBLISHED, pageable
        )).thenReturn(eventPage);

        Page<EventResponse> responses =
                eventService.getPublishedEvents(pageable);

        assertEquals(2, responses.getTotalElements());
        assertEquals(1, responses.getTotalPages());
        assertEquals("Java Backend Workshop",
                responses.getContent().get(0).getTitle());
        assertEquals("Spring Boot Seminar",
                responses.getContent().get(1).getTitle());
    }

    // 6. Get organiser's events

    @Test
    void shouldGetOrganiserEvents() {
        User organiser = createOrganiser();
        Event event = createEvent(organiser);

        when(userRepository.findByEmail(organiserEmail))
                .thenReturn(Optional.of(organiser));

        when(eventRepository.findByOrganiserIdAndDeletedFalse(1L))
                .thenReturn(List.of(event));

        List<EventResponse> responses =
                eventService.getMyEvents(organiserEmail);

        assertEquals(1, responses.size());
        assertEquals(eventId, responses.get(0).getId());
    }

    // 7. Update draft event successfully

    @Test
    void shouldUpdateDraftEventSuccessfully() {
        User organiser = createOrganiser();
        Event event = createEvent(organiser);
        EventRequest request = createRequest();

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        when(userRepository.findByEmail(organiserEmail))
                .thenReturn(Optional.of(organiser));

        when(eventRepository.save(event)).thenReturn(event);

        EventResponse response =
                eventService.updateEvent(eventId, request, organiserEmail);

        assertEquals("Updated Java Workshop", response.getTitle());
        assertEquals("Mumbai", response.getVenue());
        assertEquals(100, response.getMaxCapacity());
        assertEquals(new BigDecimal("599.00"), response.getPrice());
    }

    // 8. Reject update of published event

    @Test
    void shouldRejectUpdatingPublishedEvent() {
        User organiser = createOrganiser();
        Event event = createEvent(organiser);
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        when(userRepository.findByEmail(organiserEmail))
                .thenReturn(Optional.of(organiser));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventService.updateEvent(
                        eventId, createRequest(), organiserEmail
                )
        );

        assertEquals("Only DRAFT events can be edited", exception.getMessage());

        verify(eventRepository, never()).save(event);
    }

    // 9. Publish draft event

    @Test
    void shouldPublishDraftEvent() {
        User organiser = createOrganiser();
        Event event = createEvent(organiser);

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        when(userRepository.findByEmail(organiserEmail))
                .thenReturn(Optional.of(organiser));

        when(eventRepository.save(event)).thenReturn(event);

        EventResponse response =
                eventService.publishEvent(eventId, organiserEmail);

        assertEquals(EventStatus.PUBLISHED.name(), response.getStatus());
        verify(eventRepository).save(event);
    }

    // 10. Reject publishing an already published event

    @Test
    void shouldRejectPublishingAlreadyPublishedEvent() {
        User organiser = createOrganiser();
        Event event = createEvent(organiser);
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        when(userRepository.findByEmail(organiserEmail))
                .thenReturn(Optional.of(organiser));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventService.publishEvent(eventId, organiserEmail)
        );

        assertEquals("Only DRAFT events can be published", exception.getMessage());
    }

    // 11. Unpublish published event

    @Test
    void shouldUnpublishPublishedEvent() {
        User organiser = createOrganiser();
        Event event = createEvent(organiser);
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        when(userRepository.findByEmail(organiserEmail))
                .thenReturn(Optional.of(organiser));

        when(eventRepository.save(event)).thenReturn(event);

        EventResponse response =
                eventService.unpublishEvent(eventId, organiserEmail);

        assertEquals(EventStatus.DRAFT.name(), response.getStatus());
    }

    // 12. Cancel event successfully

    @Test
    void shouldCancelEventSuccessfully() {
        User organiser = createOrganiser();
        Event event = createEvent(organiser);

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        when(userRepository.findByEmail(organiserEmail))
                .thenReturn(Optional.of(organiser));

        when(eventRepository.save(event)).thenReturn(event);

        EventResponse response =
                eventService.cancelEvent(eventId, organiserEmail);

        assertEquals(EventStatus.CANCELLED.name(), response.getStatus());
    }

    // 13. Reject cancellation of completed event

    @Test
    void shouldRejectCancellingCompletedEvent() {
        User organiser = createOrganiser();
        Event event = createEvent(organiser);
        event.setStatus(EventStatus.COMPLETED);

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        when(userRepository.findByEmail(organiserEmail))
                .thenReturn(Optional.of(organiser));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventService.cancelEvent(eventId, organiserEmail)
        );

        assertEquals(
                "Event cannot be cancelled in its current state",
                exception.getMessage()
        );
    }

    // 14. Reject cancellation of already cancelled event

    @Test
    void shouldRejectCancellingAlreadyCancelledEvent() {
        User organiser = createOrganiser();
        Event event = createEvent(organiser);
        event.setStatus(EventStatus.CANCELLED);

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        when(userRepository.findByEmail(organiserEmail))
                .thenReturn(Optional.of(organiser));

        assertThrows(
                RuntimeException.class,
                () -> eventService.cancelEvent(eventId, organiserEmail)
        );
    }

    // 15. Soft delete event

    @Test
    void shouldSoftDeleteEvent() {
        User organiser = createOrganiser();
        Event event = createEvent(organiser);

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        when(userRepository.findByEmail(organiserEmail))
                .thenReturn(Optional.of(organiser));

        when(eventRepository.save(event)).thenReturn(event);

        eventService.deleteEvent(eventId, organiserEmail);

        assertTrue(event.isDeleted());
        verify(eventRepository).save(event);
    }

    // 16. Toggle featured from false to true

    @Test
    void shouldEnableFeaturedEvent() {
        Event event = createEvent(createOrganiser());

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        when(eventRepository.save(event)).thenReturn(event);

        EventResponse response = eventService.toggleFeatured(eventId);

        assertTrue(response.isFeatured());
    }

    // 17. Toggle featured from true to false

    @Test
    void shouldDisableFeaturedEvent() {
        Event event = createEvent(createOrganiser());
        event.setFeatured(true);

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        when(eventRepository.save(event)).thenReturn(event);

        EventResponse response = eventService.toggleFeatured(eventId);

        assertFalse(response.isFeatured());
    }

    // 18. Reject modification by non-owner

    @Test
    void shouldRejectModificationByNonOwner() {
        User organiser = createOrganiser();
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setEmail("another@test.com");
        anotherUser.setRole(Role.ORGANISER);

        Event event = createEvent(organiser);

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        when(userRepository.findByEmail("another@test.com"))
                .thenReturn(Optional.of(anotherUser));

        assertThrows(
                AccessDeniedException.class,
                () -> eventService.updateEvent(
                        eventId, createRequest(), "another@test.com"
                )
        );

        verify(eventRepository, never()).save(event);
    }

    // 19. Allow admin to modify another organiser's event

    @Test
    void shouldAllowAdminToModifyAnotherOrganisersEvent() {
        User organiser = createOrganiser();

        User admin = new User();
        admin.setId(2L);
        admin.setEmail("admin@test.com");
        admin.setRole(Role.ADMIN);

        Event event = createEvent(organiser);

        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.of(event));

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));

        when(eventRepository.save(event)).thenReturn(event);

        EventResponse response = eventService.updateEvent(
                eventId, createRequest(), "admin@test.com"
        );

        assertEquals("Updated Java Workshop", response.getTitle());
        verify(eventRepository).save(event);
    }

    // 20. Reject request when event does not exist

    @Test
    void shouldRejectWhenEventDoesNotExist() {
        when(eventRepository.findByIdAndDeletedFalse(eventId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventService.getEvent(eventId)
        );

        assertEquals("Event not found", exception.getMessage());
    }

    // 21. Reject getMyEvents when organiser does not exist

    @Test
    void shouldRejectGettingMyEventsWhenUserDoesNotExist() {
        when(userRepository.findByEmail(organiserEmail))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventService.getMyEvents(organiserEmail)
        );

        assertEquals("User not found", exception.getMessage());
    }
}
