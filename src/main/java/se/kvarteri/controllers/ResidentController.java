package se.kvarteri.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.kvarteri.dto.Dtos.ResidentSummary;
import se.kvarteri.models.Resident;
import se.kvarteri.repositories.ResidentRepository;
import se.kvarteri.services.CurrentUser;
import org.springframework.security.core.Authentication;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/residents")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentRepository residents;
    private final CurrentUser currentUser;

    /** For board members picking a tenant. Only id, name and apartment are exposed (no emails). */
    @GetMapping
    @PreAuthorize("hasAnyRole('BOARD_MEMBER','ADMIN')")
    List<ResidentSummary> list(Authentication auth) {
        return residents.findByAssociationAndEnabledTrueOrderByNameAsc(currentUser.get(auth).getAssociation()).stream()
                .map(r -> new ResidentSummary(r.getId(), r.getName(), r.getApartmentNumber(), r.getRole()))
                .toList();
    }
}
