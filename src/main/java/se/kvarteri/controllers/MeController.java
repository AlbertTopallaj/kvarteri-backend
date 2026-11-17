package se.kvarteri.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import se.kvarteri.dto.Dtos.ResidentResponse;
import se.kvarteri.services.CurrentUser;
import se.kvarteri.services.ResidentService;

/** Lets a frontend verify credentials, learn the user's role and delete one's own account. */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final CurrentUser currentUser;
    private final ResidentService residentService;

    @GetMapping
    ResidentResponse me(Authentication auth) {
        return ResidentResponse.of(currentUser.get(auth));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteMe(Authentication auth) {
        residentService.deactivate(currentUser.get(auth));
    }
}
