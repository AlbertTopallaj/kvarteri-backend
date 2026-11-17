package se.kvarteri.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.kvarteri.dto.Dtos.ResidentResponse;
import se.kvarteri.services.CurrentUser;

/** Lets a frontend verify credentials and learn the user's role. */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final CurrentUser currentUser;

    @GetMapping
    ResidentResponse me(Authentication auth) {
        return ResidentResponse.of(currentUser.get(auth));
    }
}
