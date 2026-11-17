package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.kvarteri.models.Parking;

import java.util.UUID;

public interface ParkingRepository extends JpaRepository<Parking, UUID> {
}
