package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.BookingRequest;
import se.kvarteri.exception.ConflictException;
import se.kvarteri.exception.NotFoundException;
import se.kvarteri.models.*;
import se.kvarteri.repositories.*;
import se.kvarteri.services.CurrentUser;
import se.kvarteri.util.BookingUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Booking of community rooms, laundry, parking and garages. Concurrent bookings are guarded by
 * optimistic locking (@Version) and mapped to 409 in GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Transactional
public class BookingController {

    private static final int MAX_DAYS_AHEAD = 60;

    public record ResourceView(UUID id, String name, int price, boolean booked, boolean mine,
                               LocalDateTime bookedTime) {
    }

    private final CommunityRoomRepository rooms;
    private final LaundryRepository laundries;
    private final ParkingRepository parkings;
    private final GarageRepository garages;
    private final CurrentUser currentUser;

    // ---- community rooms
    @GetMapping("/communityrooms")
    @Transactional(readOnly = true)
    List<ResourceView> listRooms(Authentication auth) {
        Resident me = currentUser.get(auth);
        return rooms.findAll().stream().map(r -> new ResourceView(r.getId(), r.getName(), r.getPrice(),
                r.getTenant() != null, isMe(r.getTenant(), me), r.getBookedTime())).toList();
    }

    @PostMapping("/communityrooms/{id}/book")
    ResourceView bookRoom(@PathVariable UUID id, @Valid @RequestBody BookingRequest req, Authentication auth) {
        Resident me = currentUser.get(auth);
        checkTime(req.time());
        CommunityRoom r = rooms.findById(id).orElseThrow(() -> new NotFoundException("Room not found"));
        if (r.getTenant() != null) throw new ConflictException("Already booked");
        r.setTenant(me);
        r.setBookedTime(req.time());
        return new ResourceView(r.getId(), r.getName(), r.getPrice(), true, true, r.getBookedTime());
    }

    @PostMapping("/communityrooms/{id}/cancel")
    void cancelRoom(@PathVariable UUID id, Authentication auth) {
        Resident me = currentUser.get(auth);
        CommunityRoom r = rooms.findById(id).orElseThrow(() -> new NotFoundException("Room not found"));
        requireOwnerOrStaff(r.getTenant(), me);
        r.setTenant(null);
        r.setBookedTime(null);
    }

    // ---- laundry
    @GetMapping("/laundries")
    @Transactional(readOnly = true)
    List<ResourceView> listLaundries(Authentication auth) {
        Resident me = currentUser.get(auth);
        return laundries.findAll().stream().map(l -> new ResourceView(l.getId(), l.getName(), 0,
                l.getBookedBy() != null, isMe(l.getBookedBy(), me), l.getBookedTime())).toList();
    }

    @PostMapping("/laundries/{id}/book")
    ResourceView bookLaundry(@PathVariable UUID id, @Valid @RequestBody BookingRequest req, Authentication auth) {
        Resident me = currentUser.get(auth);
        checkTime(req.time());
        Laundry l = laundries.findById(id).orElseThrow(() -> new NotFoundException("Laundry not found"));
        if (l.getBookedBy() != null) throw new ConflictException("Already booked");
        l.setBookedBy(me);
        l.setBookedTime(req.time());
        return new ResourceView(l.getId(), l.getName(), 0, true, true, l.getBookedTime());
    }

    @PostMapping("/laundries/{id}/cancel")
    void cancelLaundry(@PathVariable UUID id, Authentication auth) {
        Resident me = currentUser.get(auth);
        Laundry l = laundries.findById(id).orElseThrow(() -> new NotFoundException("Laundry not found"));
        requireOwnerOrStaff(l.getBookedBy(), me);
        l.setBookedBy(null);
        l.setBookedTime(null);
    }

    // ---- parking
    @GetMapping("/parkings")
    @Transactional(readOnly = true)
    List<ResourceView> listParkings(Authentication auth) {
        Resident me = currentUser.get(auth);
        return parkings.findAll().stream().map(p -> new ResourceView(p.getId(),
                "P" + p.getParkingNumber(), p.getPrice(), p.getTenant() != null, isMe(p.getTenant(), me),
                null)).toList();
    }

    @PostMapping("/parkings/{id}/claim")
    ResourceView claimParking(@PathVariable UUID id, Authentication auth) {
        Resident me = currentUser.get(auth);
        Parking p = parkings.findById(id).orElseThrow(() -> new NotFoundException("Parking not found"));
        if (p.getTenant() != null) throw new ConflictException("Already taken");
        p.setTenant(me);
        return new ResourceView(p.getId(), "P" + p.getParkingNumber(), p.getPrice(), true, true, null);
    }

    @PostMapping("/parkings/{id}/release")
    void releaseParking(@PathVariable UUID id, Authentication auth) {
        Resident me = currentUser.get(auth);
        Parking p = parkings.findById(id).orElseThrow(() -> new NotFoundException("Parking not found"));
        requireOwnerOrStaff(p.getTenant(), me);
        p.setTenant(null);
    }

    // ---- garage
    @GetMapping("/garages")
    @Transactional(readOnly = true)
    List<ResourceView> listGarages(Authentication auth) {
        Resident me = currentUser.get(auth);
        return garages.findAll().stream().map(g -> new ResourceView(g.getId(),
                "G" + g.getGarageNumber(), g.getPrice(), g.getOwner() != null, isMe(g.getOwner(), me),
                null)).toList();
    }

    @PostMapping("/garages/{id}/claim")
    ResourceView claimGarage(@PathVariable UUID id, Authentication auth) {
        Resident me = currentUser.get(auth);
        Garage g = garages.findById(id).orElseThrow(() -> new NotFoundException("Garage not found"));
        if (g.getOwner() != null) throw new ConflictException("Already taken");
        g.setOwner(me);
        return new ResourceView(g.getId(), "G" + g.getGarageNumber(), g.getPrice(), true, true, null);
    }

    @PostMapping("/garages/{id}/release")
    void releaseGarage(@PathVariable UUID id, Authentication auth) {
        Resident me = currentUser.get(auth);
        Garage g = garages.findById(id).orElseThrow(() -> new NotFoundException("Garage not found"));
        requireOwnerOrStaff(g.getOwner(), me);
        g.setOwner(null);
    }

    // ---- helpers
    private static boolean isMe(Resident owner, Resident me) {
        return owner != null && owner.getId().equals(me.getId());
    }

    private static void requireOwnerOrStaff(Resident owner, Resident me) {
        if (owner == null) throw new ConflictException("Not booked");
        if (!isMe(owner, me) && !CurrentUser.isStaff(me)) {
            throw new AccessDeniedException("Not your booking");
        }
    }

    private static void checkTime(LocalDateTime t) {
        if (!BookingUtil.isValidBookingTime(t, LocalDateTime.now(), MAX_DAYS_AHEAD)) {
            throw new IllegalArgumentException("Invalid booking time");
        }
    }
}
