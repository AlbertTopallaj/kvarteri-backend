package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.kvarteri.models.Booking;
import se.kvarteri.models.ResourceType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /** True if an existing booking overlaps [start, end). */
    boolean existsByResourceTypeAndResourceIdAndStartTimeLessThanAndEndTimeGreaterThan(
            ResourceType type, UUID resourceId, LocalDateTime end, LocalDateTime start);

    List<Booking> findByResourceTypeAndResourceIdAndEndTimeAfterOrderByStartTime(
            ResourceType type, UUID resourceId, LocalDateTime after);
}
