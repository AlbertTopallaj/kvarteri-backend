package se.kvarteri.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.kvarteri.dto.Dtos.ResidentSummary;
import se.kvarteri.models.Resident;
import se.kvarteri.repositories.ResidentRepository;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/residents")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentRepository residents;

    /** For board members picking a tenant. Only id, name and apartment are exposed (no emails). */
    @GetMapping
    @PreAuthorize("hasAnyRole('BOARD_MEMBER','ADMIN')")
    List<ResidentSummary> list() {
        return residents.findAll().stream()
                .filter(Resident::isEnabled)
                .sorted(Comparator.comparing(Resident::getName, String.CASE_INSENSITIVE_ORDER))
                .map(r -> new ResidentSummary(r.getId(), r.getName(), r.getApartmentNumber(), r.getRole()))
                .toList();
    }
}
