package se.kvarteri.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.ResourceView;
import se.kvarteri.exception.ConflictException;
import se.kvarteri.exception.NotFoundException;
import se.kvarteri.models.Garage;
import se.kvarteri.models.Parking;
import se.kvarteri.models.Resident;
import se.kvarteri.repositories.GarageRepository;
import se.kvarteri.repositories.ParkingRepository;
import se.kvarteri.services.CurrentUser;

import java.util.List;
import java.util.UUID;

/** Parking spots and garages are claimed by one resident at a time. Races are caught by @Version (409). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Transactional
public class ParkingGarageController {

    private final ParkingRepository parkings;
    private final GarageRepository garages;
    private final CurrentUser currentUser;

    @GetMapping("/parkings")
    @Transactional(readOnly = true)
    List<ResourceView> listParkings(Authentication auth) {
        Resident me = currentUser.get(auth);
        return parkings.findAll().stream().map(p -> new ResourceView(p.getId(), "P" + p.getParkingNumber(),
                p.getPrice(), p.getTenant() != null, isMe(p.getTenant(), me))).toList();
    }

    @PostMapping("/parkings/{id}/claim")
    ResourceView claimParking(@PathVariable UUID id, Authentication auth) {
        Resident me = currentUser.get(auth);
        Parking p = parkings.findById(id).orElseThrow(() -> new NotFoundException("Parking not found"));
        if (p.getTenant() != null) throw new ConflictException("Already taken");
        p.setTenant(me);
        return new ResourceView(p.getId(), "P" + p.getParkingNumber(), p.getPrice(), true, true);
    }

    @PostMapping("/parkings/{id}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void releaseParking(@PathVariable UUID id, Authentication auth) {
        Resident me = currentUser.get(auth);
        Parking p = parkings.findById(id).orElseThrow(() -> new NotFoundException("Parking not found"));
        requireOwnerOrStaff(p.getTenant(), me);
        p.setTenant(null);
    }

    @GetMapping("/garages")
    @Transactional(readOnly = true)
    List<ResourceView> listGarages(Authentication auth) {
        Resident me = currentUser.get(auth);
        return garages.findAll().stream().map(g -> new ResourceView(g.getId(), "G" + g.getGarageNumber(),
                g.getPrice(), g.getOwner() != null, isMe(g.getOwner(), me))).toList();
    }

    @PostMapping("/garages/{id}/claim")
    ResourceView claimGarage(@PathVariable UUID id, Authentication auth) {
        Resident me = currentUser.get(auth);
        Garage g = garages.findById(id).orElseThrow(() -> new NotFoundException("Garage not found"));
        if (g.getOwner() != null) throw new ConflictException("Already taken");
        g.setOwner(me);
        return new ResourceView(g.getId(), "G" + g.getGarageNumber(), g.getPrice(), true, true);
    }

    @PostMapping("/garages/{id}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void releaseGarage(@PathVariable UUID id, Authentication auth) {
        Resident me = currentUser.get(auth);
        Garage g = garages.findById(id).orElseThrow(() -> new NotFoundException("Garage not found"));
        requireOwnerOrStaff(g.getOwner(), me);
        g.setOwner(null);
    }

    private static boolean isMe(Resident owner, Resident me) {
        return owner != null && owner.getId().equals(me.getId());
    }

    private static void requireOwnerOrStaff(Resident owner, Resident me) {
        if (owner == null) throw new ConflictException("Not claimed");
        if (!isMe(owner, me) && !CurrentUser.isStaff(me)) {
            throw new AccessDeniedException("Not yours");
        }
    }
}
