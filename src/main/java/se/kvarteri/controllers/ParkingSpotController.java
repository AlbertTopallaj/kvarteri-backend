package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.*;
import se.kvarteri.exception.ConflictException;
import se.kvarteri.exception.NotFoundException;
import se.kvarteri.models.*;
import se.kvarteri.repositories.*;
import se.kvarteri.services.CurrentUser;
import se.kvarteri.services.NotificationService;

import java.util.*;

/**
 * Parking spots and garages rented indefinitely. Residents queue; board members/admins assign and terminate.
 * Every mutation locks the spot row, so queue/assign/terminate on one spot never interleave.
 */
@RestController
@RequestMapping("/api/parking-spots")
@RequiredArgsConstructor
@Transactional
public class ParkingSpotController {

    private static final String STAFF = "hasAnyRole('BOARD_MEMBER','ADMIN')";

    private final ParkingSpotRepository spots;
    private final SpotQueueEntryRepository queue;
    private final ResidentRepository residents;
    private final NotificationService notifications;
    private final CurrentUser currentUser;

    @GetMapping
    @Transactional(readOnly = true)
    List<SpotView> list(Authentication auth) {
        Resident me = currentUser.get(auth);
        Map<UUID, List<SpotQueueEntry>> byspot = new HashMap<>();
        for (SpotQueueEntry q : queue.findAllOrdered(me.getAssociation())) {
            byspot.computeIfAbsent(q.getSpot().getId(), k -> new ArrayList<>()).add(q);
        }
        return spots.findAllWithTenant(me.getAssociation()).stream()
                .map(s -> view(s, byspot.getOrDefault(s.getId(), List.of()), me)).toList();
    }

    // ---- residents

    @PostMapping("/{id}/queue")
    @ResponseStatus(HttpStatus.CREATED)
    SpotView joinQueue(@PathVariable UUID id, Authentication auth) {
        Resident me = currentUser.get(auth);
        ParkingSpot s = lock(id, me);
        if (s.getTenant() != null && s.getTenant().getId().equals(me.getId())) {
            throw new ConflictException("You already rent this spot");
        }
        if (queue.existsBySpotIdAndResidentId(id, me.getId())) {
            throw new ConflictException("You are already in the queue");
        }
        SpotQueueEntry e = new SpotQueueEntry();
        e.setSpot(s);
        e.setResident(me);
        queue.saveAndFlush(e);
        return view(s, queue.findBySpotOrdered(s), me);
    }

    @DeleteMapping("/{id}/queue")
    SpotView leaveQueue(@PathVariable UUID id, Authentication auth) {
        Resident me = currentUser.get(auth);
        ParkingSpot s = lock(id, me);
        SpotQueueEntry e = queue.findBySpotIdAndResidentId(id, me.getId())
                .orElseThrow(() -> new NotFoundException("You are not in the queue"));
        queue.delete(e);
        queue.flush();
        return view(s, queue.findBySpotOrdered(s), me);
    }

    // ---- board members / admins

    @PostMapping
    @PreAuthorize(STAFF)
    @ResponseStatus(HttpStatus.CREATED)
    SpotView create(@Valid @RequestBody CreateSpotRequest req, Authentication auth) {
        Resident me = currentUser.get(auth);
        ParkingSpot s = new ParkingSpot();
        s.setAssociation(me.getAssociation());
        s.setKind(req.kind());
        s.setName(req.name().trim());
        s.setPrice(req.price());
        s = spots.saveAndFlush(s);
        return view(s, List.of(), me);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(STAFF)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id, Authentication auth) {
        Resident me = currentUser.get(auth);
        ParkingSpot s = lock(id, me);
        if (s.getTenant() != null) throw new ConflictException("Spot is rented, terminate it first");
        queue.deleteBySpot(s);
        spots.delete(s);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(STAFF)
    SpotView changePrice(@PathVariable UUID id, @Valid @RequestBody PriceRequest req, Authentication auth) {
        Resident me = currentUser.get(auth);
        ParkingSpot s = lock(id, me);
        s.setPrice(req.price());
        return view(s, queue.findBySpotOrdered(s), me);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize(STAFF)
    SpotView assign(@PathVariable UUID id, @Valid @RequestBody AssignRequest req, Authentication auth) {
        Resident staff = currentUser.get(auth);
        ParkingSpot s = lock(id, staff);
        if (s.getTenant() != null) throw new ConflictException("Spot is already rented");
        Resident tenant = residents.findById(req.residentId())
                .orElseThrow(() -> new NotFoundException("Resident not found"));
        if (!tenant.isEnabled() || !CurrentUser.sameAssociation(tenant, staff)) {
            throw new NotFoundException("Resident not found");
        }
        s.setTenant(tenant);
        queue.findBySpotIdAndResidentId(id, tenant.getId()).ifPresent(queue::delete);
        queue.flush();
        notifications.notify(tenant, "Du har fått " + s.getName() + " (" + s.getPrice() + " kr/mån).");
        return view(s, queue.findBySpotOrdered(s), staff);
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize(STAFF)
    SpotView terminate(@PathVariable UUID id, Authentication auth) {
        Resident staff = currentUser.get(auth);
        ParkingSpot s = lock(id, staff);
        Resident old = s.getTenant();
        if (old == null) throw new ConflictException("Spot is not rented");
        s.setTenant(null);
        notifications.notify(old, "Ditt hyresavtal för " + s.getName() + " har sagts upp.");
        List<SpotQueueEntry> q = queue.findBySpotOrdered(s);
        if (!q.isEmpty()) {
            notifications.notify(q.get(0).getResident(), s.getName() + " är nu ledig – du står först i kön!");
        }
        return view(s, q, staff);
    }

    // ---- helpers

    private ParkingSpot lock(UUID id, Resident me) {
        return spots.findByIdForUpdate(id, me.getAssociation()).orElseThrow(() -> new NotFoundException("Spot not found"));
    }



    private static SpotView view(ParkingSpot s, List<SpotQueueEntry> q, Resident me) {
        List<QueueMember> members = q.stream()
                .map(e -> new QueueMember(e.getResident().getId(), e.getResident().getName())).toList();
        int pos = 0;
        Integer myPos = null;
        for (SpotQueueEntry e : q) {
            pos++;
            if (e.getResident().getId().equals(me.getId())) myPos = pos;
        }
        boolean mine = s.getTenant() != null && s.getTenant().getId().equals(me.getId());
        return new SpotView(s.getId(), s.getKind(), s.getName(), s.getPrice(), TenantView.of(s.getTenant()),
                members, members.size(), myPos, mine);
    }
}
