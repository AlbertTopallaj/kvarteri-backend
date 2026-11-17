package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.kvarteri.models.Garage;

import java.util.UUID;

public interface GarageRepository extends JpaRepository<Garage, UUID> {
}
