package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.*;
import se.kvarteri.models.CommunityRoom;
import se.kvarteri.models.Laundry;
import se.kvarteri.repositories.CommunityRoomRepository;
import se.kvarteri.repositories.LaundryRepository;

/** Admin creates the bookable resources of a complex. Parking spots are managed via /api/parking-spots. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminResourceController {

    private final CommunityRoomRepository rooms;
    private final LaundryRepository laundries;

    @PostMapping("/communityrooms")
    @ResponseStatus(HttpStatus.CREATED)
    ResourceView room(@Valid @RequestBody RoomRequest req) {
        CommunityRoom r = new CommunityRoom();
        r.setName(req.name());
        r.setPrice(req.price());
        r.setCapacity(req.capacity());
        r = rooms.save(r);
        return new ResourceView(r.getId(), r.getName(), r.getPrice(), false, false);
    }

    @PostMapping("/laundries")
    @ResponseStatus(HttpStatus.CREATED)
    ResourceView laundry(@Valid @RequestBody LaundryRequest req) {
        Laundry l = new Laundry();
        l.setName(req.name());
        l = laundries.save(l);
        return new ResourceView(l.getId(), l.getName(), 0, false, false);
    }
}
