package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.kvarteri.models.Association;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssociationRepository extends JpaRepository<Association, UUID> {

    Optional<Association> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Association> findAllByOrderByNameAsc();
}
