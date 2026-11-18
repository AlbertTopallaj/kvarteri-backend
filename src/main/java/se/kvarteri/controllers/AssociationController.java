package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.AssociationView;
import se.kvarteri.dto.Dtos.CreateAssociationRequest;
import se.kvarteri.exception.ConflictException;
import se.kvarteri.models.Association;
import se.kvarteri.repositories.AssociationRepository;

import java.util.List;

/** Admin (dev) manages the associations. Enforced as ADMIN-only by SecurityConfig. */
@RestController
@RequestMapping("/api/admin/associations")
@RequiredArgsConstructor
public class AssociationController {

    private final AssociationRepository associations;

    @GetMapping
    List<AssociationView> list() {
        return associations.findAllByOrderByNameAsc().stream().map(AssociationView::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AssociationView create(@Valid @RequestBody CreateAssociationRequest req) {
        String name = req.name().trim();
        if (associations.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Association already exists");
        }
        Association a = new Association();
        a.setName(name);
        return AssociationView.of(associations.save(a));
    }
}
