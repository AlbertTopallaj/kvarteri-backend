package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.*;
import se.kvarteri.models.ResourceType;
import se.kvarteri.repositories.CommunityRoomRepository;
import se.kvarteri.repositories.LaundryRepository;
import se.kvarteri.services.BookingService;
import se.kvarteri.services.CurrentUser;

import java.util.List;
import java.util.UUID;

/** Time-slot booking of community rooms and laundry rooms. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService service;
    private final CurrentUser currentUser;
    private final CommunityRoomRepository rooms;
    private final LaundryRepository laundries;

    @GetMapping("/communityrooms")
    List<ResourceView> listRooms() {
        return rooms.findAll().stream()
                .map(r -> new ResourceView(r.getId(), r.getName(), r.getPrice(), false, false)).toList();
    }

    @GetMapping("/laundries")
    List<ResourceView> listLaundries() {
        return laundries.findAll().stream()
                .map(l -> new ResourceView(l.getId(), l.getName(), 0, false, false)).toList();
    }

    @GetMapping("/communityrooms/{id}/bookings")
    List<BookingResponse> roomBookings(@PathVariable UUID id, Authentication auth) {
        return service.upcoming(ResourceType.COMMUNITY_ROOM, id, currentUser.get(auth));
    }

    @PostMapping("/communityrooms/{id}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    BookingResponse bookRoom(@PathVariable UUID id, @Valid @RequestBody BookingRequest req, Authentication auth) {
        return service.book(ResourceType.COMMUNITY_ROOM, id, req.start(), req.end(), currentUser.get(auth));
    }

    @GetMapping("/laundries/{id}/bookings")
    List<BookingResponse> laundryBookings(@PathVariable UUID id, Authentication auth) {
        return service.upcoming(ResourceType.LAUNDRY, id, currentUser.get(auth));
    }

    @PostMapping("/laundries/{id}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    BookingResponse bookLaundry(@PathVariable UUID id, @Valid @RequestBody BookingRequest req,
                                Authentication auth) {
        return service.book(ResourceType.LAUNDRY, id, req.start(), req.end(), currentUser.get(auth));
    }

    @DeleteMapping("/bookings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable UUID id, Authentication auth) {
        service.cancel(id, currentUser.get(auth));
    }
}
