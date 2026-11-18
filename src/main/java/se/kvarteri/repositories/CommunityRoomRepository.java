package se.kvarteri.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import se.kvarteri.models.Association;
import se.kvarteri.models.CommunityRoom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunityRoomRepository extends JpaRepository<CommunityRoom, UUID> {

    List<CommunityRoom> findByAssociationOrderByNameAsc(Association association);

    boolean existsByIdAndAssociation(UUID id, Association association);

    /** Locks the row so concurrent bookings of the same resource are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CommunityRoom r where r.id = ?1 and r.association = ?2")
    Optional<CommunityRoom> findByIdForUpdate(UUID id, Association association);
}
