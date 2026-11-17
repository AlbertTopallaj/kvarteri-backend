package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.kvarteri.models.CommunityRoom;

import java.util.UUID;

public interface CommunityRoomRepository extends JpaRepository<CommunityRoom, UUID> {
}
