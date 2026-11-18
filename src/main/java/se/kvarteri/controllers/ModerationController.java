package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.WarningRequest;
import se.kvarteri.dto.Dtos.WarningResponse;
import se.kvarteri.exception.ConflictException;
import se.kvarteri.exception.NotFoundException;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Role;
import se.kvarteri.models.Warning;
import se.kvarteri.repositories.ResidentRepository;
import se.kvarteri.repositories.WarningRepository;
import se.kvarteri.services.CurrentUser;
import se.kvarteri.services.NotificationService;
import se.kvarteri.services.ResidentService;

import java.util.List;
import java.util.UUID;

/**
 * Board members remove and warn residents of their own association. Admins may do it for anyone.
 * Board members can only act on RESIDENT accounts, never on other board members or admins.
 */
@RestController
@RequestMapping("/api/residents/{id}")
@RequiredArgsConstructor
@Transactional
@PreAuthorize("hasAnyRole('BOARD_MEMBER','ADMIN')")
public class ModerationController {

    private final ResidentRepository residents;
    private final WarningRepository warnings;
    private final ResidentService residentService;
    private final NotificationService notifications;
    private final CurrentUser currentUser;

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable UUID id, Authentication auth) {
        residentService.deactivate(target(id, currentUser.get(auth)));
    }

    @PostMapping("/warnings")
    @ResponseStatus(HttpStatus.CREATED)
    WarningResponse warn(@PathVariable UUID id, @Valid @RequestBody WarningRequest req, Authentication auth) {
        Resident staff = currentUser.get(auth);
        Resident target = target(id, staff);
        Warning w = new Warning();
        w.setResident(target);
        w.setIssuedBy(staff);
        w.setReason(req.reason().trim());
        w = warnings.save(w);
        notifications.notify(target, "Du har fått en varning: " + w.getReason());
        return WarningResponse.of(w);
    }

    @GetMapping("/warnings")
    @Transactional(readOnly = true)
    List<WarningResponse> history(@PathVariable UUID id, Authentication auth) {
        Resident target = target(id, currentUser.get(auth));
        return warnings.findByResident(target).stream().map(WarningResponse::of).toList();
    }

    /** Same response for "missing" and "other association" so ids in other associations cannot be probed. */
    private Resident target(UUID id, Resident staff) {
        Resident t = residents.findById(id).filter(Resident::isEnabled)
                .orElseThrow(() -> new NotFoundException("Resident not found"));
        if (staff.getRole() != Role.ADMIN && !CurrentUser.sameAssociation(t, staff)) {
            throw new NotFoundException("Resident not found");
        }
        if (t.getId().equals(staff.getId())) {
            throw new ConflictException("You cannot moderate your own account");
        }
        if (staff.getRole() == Role.BOARD_MEMBER && t.getRole() != Role.RESIDENT) {
            throw new AccessDeniedException("Board members can only moderate residents");
        }
        return t;
    }
}
