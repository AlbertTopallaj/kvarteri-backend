package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.kvarteri.models.Association;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResidentRepository extends JpaRepository<Resident, UUID> {

    Optional<Resident> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Resident> findByAssociationAndRoleAndEnabledTrue(Association association, Role role);

    List<Resident> findByEnabledTrueOrderByNameAsc();

    List<Resident> findByAssociationAndEnabledTrueOrderByNameAsc(Association association);

    long countByRoleAndEnabledTrue(Role role);
}
