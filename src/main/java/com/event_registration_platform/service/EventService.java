
package com.event_registration_platform.service;

import com.event_registration_platform.dto.EventRequest;
import com.event_registration_platform.dto.EventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EventService {

    EventResponse createEvent(EventRequest request, String organiserEmail);

    EventResponse getEvent(Long id);

    Page<EventResponse> getPublishedEvents(Pageable pageable);

    List<EventResponse> getMyEvents(String organiserEmail);

    EventResponse updateEvent(Long id, EventRequest request, String organiserEmail);

    EventResponse publishEvent(Long id, String organiserEmail);

    EventResponse unpublishEvent(Long id, String organiserEmail);

    EventResponse cancelEvent(Long id, String organiserEmail);

    void deleteEvent(Long id, String organiserEmail);

    EventResponse toggleFeatured(Long id);
}
