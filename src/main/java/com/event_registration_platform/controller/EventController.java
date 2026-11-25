
package com.event_registration_platform.controller;

import com.event_registration_platform.dto.EventRequest;
import com.event_registration_platform.dto.EventResponse;
import com.event_registration_platform.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public EventResponse create(@Valid @RequestBody EventRequest request, Authentication auth) {
        return eventService.createEvent(request, auth.getName());
    }

    @GetMapping
    public Page<EventResponse> published(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return eventService.getPublishedEvents(pageable);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return eventService.getEvent(id);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public List<EventResponse> mine(Authentication auth) {
        return eventService.getMyEvents(auth.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request, Authentication auth) {
        return eventService.updateEvent(id, request, auth.getName());
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public EventResponse publish(@PathVariable Long id, Authentication auth) {
        return eventService.publishEvent(id, auth.getName());
    }

    @PatchMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public EventResponse unpublish(@PathVariable Long id, Authentication auth) {
        return eventService.unpublishEvent(id, auth.getName());
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public EventResponse cancel(@PathVariable Long id, Authentication auth) {
        return eventService.cancelEvent(id, auth.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public ResponseEntity<String> delete(@PathVariable Long id, Authentication auth) {
        eventService.deleteEvent(id, auth.getName());
        return ResponseEntity.ok("Event deleted");
    }

    @PatchMapping("/{id}/feature")
    @PreAuthorize("hasRole('ADMIN')")
    public EventResponse feature(@PathVariable Long id) {
        return eventService.toggleFeatured(id);
    }
}
