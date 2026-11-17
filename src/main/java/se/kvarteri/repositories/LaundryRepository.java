package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.kvarteri.models.Laundry;

import java.util.UUID;

public interface LaundryRepository extends JpaRepository<Laundry, UUID> {
}
