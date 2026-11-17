package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.CreateResidentRequest;
import se.kvarteri.dto.Dtos.ResidentResponse;
import se.kvarteri.exception.ConflictException;
import se.kvarteri.exception.NotFoundException;
import se.kvarteri.models.Resident;
import se.kvarteri.repositories.ResidentRepository;
import se.kvarteri.services.ResidentService;

import java.util.List;
import java.util.UUID;

/** The admin (dev) gives access to the system by managing accounts. */
@RestController
@RequestMapping("/api/admin/residents")
@RequiredArgsConstructor
public class AdminController {

    private final ResidentRepository residents;
    private final ResidentService residentService;
    private final PasswordEncoder encoder;

    @GetMapping
    List<ResidentResponse> list() {
        return residents.findByEnabledTrueOrderByNameAsc().stream().map(ResidentResponse::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResidentResponse create(@Valid @RequestBody CreateResidentRequest req) {
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
        return ResidentResponse.of(residents.save(r));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable UUID id) {
        Resident r = residents.findById(id).filter(Resident::isEnabled)
                .orElseThrow(() -> new NotFoundException("Resident not found"));
        residentService.deactivate(r);
    }
}
