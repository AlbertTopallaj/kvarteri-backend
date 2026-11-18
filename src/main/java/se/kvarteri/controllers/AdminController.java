package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.CreateResidentRequest;
import se.kvarteri.dto.Dtos.ResidentResponse;
import se.kvarteri.exception.ConflictException;
import se.kvarteri.exception.NotFoundException;
import se.kvarteri.models.Association;
import se.kvarteri.models.Resident;
import se.kvarteri.repositories.AssociationRepository;
import se.kvarteri.repositories.ResidentRepository;
import se.kvarteri.services.CurrentUser;
import se.kvarteri.services.ResidentService;

import java.util.List;
import java.util.UUID;

/** The admin (dev) gives access to the system by managing accounts, across all associations. */
@RestController
@RequestMapping("/api/admin/residents")
@RequiredArgsConstructor
public class AdminController {

    private final ResidentRepository residents;
    private final AssociationRepository associations;
    private final ResidentService residentService;
    private final PasswordEncoder encoder;
    private final CurrentUser currentUser;

    /** All accounts, or only one association's when associationId is given. */
    @GetMapping
    List<ResidentResponse> list(@RequestParam(required = false) UUID associationId) {
        List<Resident> result = associationId == null
                ? residents.findByEnabledTrueOrderByNameAsc()
                : residents.findByAssociationAndEnabledTrueOrderByNameAsc(association(associationId));
        return result.stream().map(ResidentResponse::of).toList();
    }

    /** The new account joins associationId, or the admin's own association if omitted. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResidentResponse create(@Valid @RequestBody CreateResidentRequest req, Authentication auth) {
        if (residents.existsByEmailIgnoreCase(req.email().trim())) {
            throw new ConflictException("Email already registered");
        }
        Resident r = new Resident();
        r.setEmail(req.email().trim().toLowerCase());
        r.setPasswordHash(encoder.encode(req.password()));
        r.setName(req.name().trim());
        r.setApartmentNumber(req.apartmentNumber() == null || req.apartmentNumber().isBlank()
                ? null : req.apartmentNumber().trim());
        r.setRole(req.role());
        r.setAssociation(req.associationId() == null
                ? currentUser.get(auth).getAssociation() : association(req.associationId()));
        return ResidentResponse.of(residents.save(r));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable UUID id) {
        Resident r = residents.findById(id).filter(Resident::isEnabled)
                .orElseThrow(() -> new NotFoundException("Resident not found"));
        residentService.deactivate(r);
    }

    private Association association(UUID id) {
        return associations.findById(id).orElseThrow(() -> new NotFoundException("Association not found"));
    }
}
