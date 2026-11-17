package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.*;
import se.kvarteri.models.CommunityRoom;
import se.kvarteri.models.Garage;
import se.kvarteri.models.Laundry;
import se.kvarteri.models.Parking;
import se.kvarteri.repositories.CommunityRoomRepository;
import se.kvarteri.repositories.GarageRepository;
import se.kvarteri.repositories.LaundryRepository;
import se.kvarteri.repositories.ParkingRepository;

/** Admin creates the bookable resources of a complex. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminResourceController {

    private final CommunityRoomRepository rooms;
    private final LaundryRepository laundries;
    private final ParkingRepository parkings;
    private final GarageRepository garages;

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

    @PostMapping("/parkings")
    @ResponseStatus(HttpStatus.CREATED)
    ResourceView parking(@Valid @RequestBody NumberedRequest req) {
        Parking p = new Parking();
        p.setParkingNumber(req.number());
        p.setPrice(req.price());
        p = parkings.save(p);
        return new ResourceView(p.getId(), "P" + p.getParkingNumber(), p.getPrice(), false, false);
    }

    @PostMapping("/garages")
    @ResponseStatus(HttpStatus.CREATED)
    ResourceView garage(@Valid @RequestBody NumberedRequest req) {
        Garage g = new Garage();
        g.setGarageNumber(req.number());
        g.setPrice(req.price());
        g = garages.save(g);
        return new ResourceView(g.getId(), "G" + g.getGarageNumber(), g.getPrice(), false, false);
    }
}
