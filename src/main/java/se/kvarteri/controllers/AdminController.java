package se.kvarteri.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.CreateResidentRequest;
import se.kvarteri.dto.Dtos.ResidentResponse;
import se.kvarteri.exception.ConflictException;
import se.kvarteri.models.Resident;
import se.kvarteri.repositories.ResidentRepository;

/** The admin (dev) gives access to the system by creating accounts. */
@RestController
@RequestMapping("/api/admin/residents")
@RequiredArgsConstructor
public class AdminController {

    private final ResidentRepository residents;
    private final PasswordEncoder encoder;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResidentResponse create(@Valid @RequestBody CreateResidentRequest req) {
        if (residents.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("Email already registered");
        }
        Resident r = new Resident();
        r.setEmail(req.email().trim().toLowerCase());
        r.setPasswordHash(encoder.encode(req.password()));
        r.setName(req.name());
        r.setApartmentNumber(req.apartmentNumber());
        r.setRole(req.role());
        return ResidentResponse.of(residents.save(r));
    }
}
