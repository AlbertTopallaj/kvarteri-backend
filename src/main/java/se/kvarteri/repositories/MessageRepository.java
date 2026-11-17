package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import se.kvarteri.models.Message;
import se.kvarteri.models.Resident;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("select m from Message m where m.recipient is null or m.recipient = ?1 order by m.createdAt desc")
    List<Message> findVisibleTo(Resident resident);
}
