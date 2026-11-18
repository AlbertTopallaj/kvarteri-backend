package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import se.kvarteri.models.ParkingSpot;
import se.kvarteri.models.SpotQueueEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpotQueueEntryRepository extends JpaRepository<SpotQueueEntry, UUID> {

    @Query("select q from SpotQueueEntry q join fetch q.resident join fetch q.spot where q.spot.association = ?1 order by q.createdAt, q.id")
    List<SpotQueueEntry> findAllOrdered(se.kvarteri.models.Association association);

    @Query("select q from SpotQueueEntry q join fetch q.resident where q.spot = ?1 order by q.createdAt, q.id")
    List<SpotQueueEntry> findBySpotOrdered(ParkingSpot spot);

    @Modifying
    @Query("delete from SpotQueueEntry q where q.resident = ?1")
    void deleteByResident(se.kvarteri.models.Resident resident);

    Optional<SpotQueueEntry> findBySpotIdAndResidentId(UUID spotId, UUID residentId);

    boolean existsBySpotIdAndResidentId(UUID spotId, UUID residentId);

    @Modifying
    @Query("delete from SpotQueueEntry q where q.spot = ?1")
    void deleteBySpot(ParkingSpot spot);
}
