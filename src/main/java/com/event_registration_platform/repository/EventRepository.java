
package com.event_registration_platform.repository;

import com.event_registration_platform.entity.Event;
import com.event_registration_platform.entity.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByIdAndDeletedFalse(Long id);

    List<Event> findByDeletedFalseAndStatus(EventStatus status);

    Page<Event> findByDeletedFalseAndStatus(
            EventStatus status,
            Pageable pageable
    );

    List<Event> findByOrganiserIdAndDeletedFalse(Long organiserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :eventId AND e.deleted = false")
    Optional<Event> findByIdAndDeletedFalseWithLock(@Param("eventId") Long eventId);
}
