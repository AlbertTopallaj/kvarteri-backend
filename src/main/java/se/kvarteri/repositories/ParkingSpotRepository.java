package se.kvarteri.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import se.kvarteri.models.ParkingSpot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, UUID> {

    @Query("select s from ParkingSpot s left join fetch s.tenant order by s.kind, s.name")
    List<ParkingSpot> findAllWithTenant();

    /** Locks the row so queue/assign/terminate on the same spot are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ParkingSpot s left join fetch s.tenant where s.id = ?1")
    Optional<ParkingSpot> findByIdForUpdate(UUID id);
}
