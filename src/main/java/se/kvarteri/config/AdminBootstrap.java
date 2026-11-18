package se.kvarteri.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import se.kvarteri.models.Association;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Role;
import se.kvarteri.repositories.AssociationRepository;
import se.kvarteri.repositories.ResidentRepository;

/** Creates the first admin from KVARTERI_ADMIN_EMAIL / KVARTERI_ADMIN_PASSWORD if no admin exists. */
@Component
@Slf4j
class AdminBootstrap implements ApplicationRunner {

    private final ResidentRepository residents;
    private final AssociationRepository associations;
    private final PasswordEncoder encoder;
    private final String associationName;
    private final String email;
    private final String password;

    AdminBootstrap(ResidentRepository residents, AssociationRepository associations, PasswordEncoder encoder,
                   @Value("${kvarteri.admin.association:Standardförening}") String associationName,
                   @Value("${kvarteri.admin.email:}") String email,
                   @Value("${kvarteri.admin.password:}") String password) {
        this.residents = residents;
        this.associations = associations;
        this.encoder = encoder;
        this.associationName = associationName;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.length() < 10) {
            return;
        }
        if (residents.existsByEmailIgnoreCase(email)) {
            return;
        }
        Resident admin = new Resident();
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(encoder.encode(password));
        admin.setName("Admin");
        admin.setRole(Role.ADMIN);
        admin.setAssociation(associations.findByNameIgnoreCase(associationName).orElseGet(() -> {
            Association a = new Association();
            a.setName(associationName);
            return associations.save(a);
        }));
        residents.save(admin);
        log.info("Created initial admin account");
    }
}
