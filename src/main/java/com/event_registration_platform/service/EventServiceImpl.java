
package com.event_registration_platform.service;

import com.event_registration_platform.dto.EventRequest;
import com.event_registration_platform.dto.EventResponse;
import com.event_registration_platform.entity.Event;
import com.event_registration_platform.entity.EventStatus;
import com.event_registration_platform.entity.Role;
import com.event_registration_platform.entity.User;
import com.event_registration_platform.exception.ResourceNotFoundException;
import com.event_registration_platform.repository.EventRepository;
import com.event_registration_platform.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventServiceImpl(EventRepository eventRepository,
                            UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Override
    public EventResponse createEvent(EventRequest request, String organiserEmail) {

        User organiser = userRepository.findByEmail(organiserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event event = new Event();
        event.setOrganiser(organiser);

        apply(event, request);

        return toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse getEvent(Long id) {

        Event event = findEvent(id);

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new RuntimeException("Published event not found");
        }

        return toResponse(event);
    }

    @Override
    public Page<EventResponse> getPublishedEvents(Pageable pageable) {

        return eventRepository
                .findByDeletedFalseAndStatus(EventStatus.PUBLISHED, pageable)
                .map(this::toResponse);
    }

    @Override
    public List<EventResponse> getMyEvents(String organiserEmail) {

        User organiser = userRepository.findByEmail(organiserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return eventRepository
                .findByOrganiserIdAndDeletedFalse(organiser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EventResponse updateEvent(Long id,
                                     EventRequest request,
                                     String organiserEmail) {

        Event event = findOwnedEvent(id, organiserEmail);

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT events can be edited");
        }

        apply(event, request);

        return toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse publishEvent(Long id, String organiserEmail) {

        Event event = findOwnedEvent(id, organiserEmail);

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT events can be published");
        }

        event.setStatus(EventStatus.PUBLISHED);

        return toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse unpublishEvent(Long id, String organiserEmail) {

        Event event = findOwnedEvent(id, organiserEmail);

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new RuntimeException("Only PUBLISHED events can be unpublished");
        }

        event.setStatus(EventStatus.DRAFT);

        return toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse cancelEvent(Long id, String organiserEmail) {

        Event event = findOwnedEvent(id, organiserEmail);

        if (event.getStatus() == EventStatus.COMPLETED ||
                event.getStatus() == EventStatus.CANCELLED) {

            throw new RuntimeException(
                    "Event cannot be cancelled in its current state");
        }

        event.setStatus(EventStatus.CANCELLED);

        return toResponse(eventRepository.save(event));
    }

    @Override
    public void deleteEvent(Long id, String organiserEmail) {

        Event event = findOwnedEvent(id, organiserEmail);

        event.setDeleted(true);

        eventRepository.save(event);
    }

    @Override
    public EventResponse toggleFeatured(Long id) {

        Event event = findEvent(id);

        event.setFeatured(!event.isFeatured());

        return toResponse(eventRepository.save(event));
    }

    private Event findEvent(Long id) {

        return eventRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Event not found"));
    }

    private Event findOwnedEvent(Long id, String email) {

        Event event = findEvent(id);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isOwner = event.getOrganiser().getId()
                .equals(user.getId());

        if (!isOwner && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(
                    "You are not allowed to modify this event");
        }

        return event;
    }

    private void apply(Event event, EventRequest request) {

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setVenue(request.getVenue());
        event.setEventDate(request.getEventDate());
        event.setMaxCapacity(request.getMaxCapacity());
        event.setPrice(request.getPrice());
    }

    private EventResponse toResponse(Event event) {

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getVenue(),
                event.getEventDate(),
                event.getMaxCapacity(),
                event.getPrice(),
                event.getStatus().name(),
                event.isFeatured(),
                event.getOrganiser().getName()
        );
    }
}
