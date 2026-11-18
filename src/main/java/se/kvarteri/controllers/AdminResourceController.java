package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.*;
import se.kvarteri.exception.NotFoundException;
import se.kvarteri.models.Association;
import se.kvarteri.models.CommunityRoom;
import se.kvarteri.models.Laundry;
import se.kvarteri.repositories.AssociationRepository;
import se.kvarteri.repositories.CommunityRoomRepository;
import se.kvarteri.repositories.LaundryRepository;
import se.kvarteri.services.CurrentUser;

import java.util.UUID;

/**
 * Admin creates the bookable resources of an association (associationId, default: the admin's own).
 * Parking spots are managed via /api/parking-spots.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminResourceController {

    private final CommunityRoomRepository rooms;
    private final LaundryRepository laundries;
    private final AssociationRepository associations;
    private final CurrentUser currentUser;

    @PostMapping("/communityrooms")
    @ResponseStatus(HttpStatus.CREATED)
    ResourceView room(@Valid @RequestBody RoomRequest req, Authentication auth) {
        CommunityRoom r = new CommunityRoom();
        r.setName(req.name());
        r.setPrice(req.price());
        r.setCapacity(req.capacity());
        r.setAssociation(association(req.associationId(), auth));
        r = rooms.save(r);
        return new ResourceView(r.getId(), r.getName(), r.getPrice(), false, false);
    }

    @PostMapping("/laundries")
    @ResponseStatus(HttpStatus.CREATED)
    ResourceView laundry(@Valid @RequestBody LaundryRequest req, Authentication auth) {
        Laundry l = new Laundry();
        l.setName(req.name());
        l.setAssociation(association(req.associationId(), auth));
        l = laundries.save(l);
        return new ResourceView(l.getId(), l.getName(), 0, false, false);
    }

    private Association association(UUID id, Authentication auth) {
        if (id == null) return currentUser.get(auth).getAssociation();
        return associations.findById(id).orElseThrow(() -> new NotFoundException("Association not found"));
    }
}
