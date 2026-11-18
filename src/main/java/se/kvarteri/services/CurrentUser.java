package se.kvarteri.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import se.kvarteri.exception.NotFoundException;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Role;
import se.kvarteri.repositories.ResidentRepository;

@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final ResidentRepository residents;

    public Resident get(Authentication auth) {
        return residents.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /** True if both users belong to the same association. */
    public static boolean sameAssociation(Resident a, Resident b) {
        return a.getAssociation().getId().equals(b.getAssociation().getId());
    }

    public static boolean isStaff(Resident r) {
        return r.getRole() == Role.BOARD_MEMBER || r.getRole() == Role.ADMIN;
    }
}
