package se.kvarteri.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.kvarteri.dto.Dtos.BookingResponse;
import se.kvarteri.exception.ConflictException;
import se.kvarteri.exception.NotFoundException;
import se.kvarteri.models.Booking;
import se.kvarteri.models.Resident;
import se.kvarteri.models.ResourceType;
import se.kvarteri.repositories.BookingRepository;
import se.kvarteri.repositories.CommunityRoomRepository;
import se.kvarteri.repositories.LaundryRepository;
import se.kvarteri.util.BookingUtil;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    static final int MAX_DAYS_AHEAD = 60;
    static final Duration MAX_LENGTH = Duration.ofHours(12);

    private final BookingRepository bookings;
    private final CommunityRoomRepository rooms;
    private final LaundryRepository laundries;
    private final Clock clock;

    public BookingResponse book(ResourceType type, UUID resourceId, LocalDateTime start, LocalDateTime end,
                                Resident me) {
        if (!BookingUtil.isValidPeriod(start, end, LocalDateTime.now(clock), MAX_DAYS_AHEAD, MAX_LENGTH)) {
            throw new IllegalArgumentException("Invalid booking period");
        }
        lockResource(type, resourceId);
        if (bookings.existsByResourceTypeAndResourceIdAndStartTimeLessThanAndEndTimeGreaterThan(
                type, resourceId, end, start)) {
            throw new ConflictException("That time is already booked");
        }
        Booking b = new Booking();
        b.setResourceType(type);
        b.setResourceId(resourceId);
        b.setResident(me);
        b.setStartTime(start);
        b.setEndTime(end);
        return view(bookings.save(b), me);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> upcoming(ResourceType type, UUID resourceId, Resident me) {
        lockFreeExists(type, resourceId);
        return bookings.findByResourceTypeAndResourceIdAndEndTimeAfterOrderByStartTime(
                type, resourceId, LocalDateTime.now(clock)).stream().map(b -> view(b, me)).toList();
    }

    public void cancel(UUID bookingId, Resident me) {
        Booking b = bookings.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (!b.getResident().getId().equals(me.getId()) && !CurrentUser.isStaff(me)) {
            throw new AccessDeniedException("Not your booking");
        }
        bookings.delete(b);
    }

    private void lockResource(ResourceType type, UUID id) {
        boolean found = switch (type) {
            case COMMUNITY_ROOM -> rooms.findByIdForUpdate(id).isPresent();
            case LAUNDRY -> laundries.findByIdForUpdate(id).isPresent();
        };
        if (!found) throw new NotFoundException("Resource not found");
    }

    private void lockFreeExists(ResourceType type, UUID id) {
        boolean found = switch (type) {
            case COMMUNITY_ROOM -> rooms.existsById(id);
            case LAUNDRY -> laundries.existsById(id);
        };
        if (!found) throw new NotFoundException("Resource not found");
    }

    private static BookingResponse view(Booking b, Resident me) {
        return new BookingResponse(b.getId(), b.getStartTime(), b.getEndTime(),
                b.getResident().getId().equals(me.getId()));
    }
}
