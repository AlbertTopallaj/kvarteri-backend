package se.kvarteri.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import se.kvarteri.models.CommunityRoom;

import java.util.Optional;
import java.util.UUID;

public interface CommunityRoomRepository extends JpaRepository<CommunityRoom, UUID> {

    /** Locks the row so concurrent bookings of the same resource are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CommunityRoom r where r.id = ?1")
    Optional<CommunityRoom> findByIdForUpdate(UUID id);
}
