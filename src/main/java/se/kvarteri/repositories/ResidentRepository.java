package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.kvarteri.models.Resident;

import java.util.Optional;
import java.util.UUID;

public interface ResidentRepository extends JpaRepository<Resident, UUID> {

    Optional<Resident> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    java.util.List<Resident> findByRoleAndEnabledTrue(se.kvarteri.models.Role role);

    java.util.List<Resident> findByEnabledTrueOrderByNameAsc();

    long countByRoleAndEnabledTrue(se.kvarteri.models.Role role);
}
