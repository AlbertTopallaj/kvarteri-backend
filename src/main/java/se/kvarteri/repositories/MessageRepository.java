package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.kvarteri.models.Association;
import se.kvarteri.models.Message;
import se.kvarteri.models.Resident;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("select m from Message m where m.author.association = :assoc "
            + "and (m.recipient is null or m.recipient = :me) order by m.createdAt desc")
    List<Message> findVisibleTo(@Param("assoc") Association assoc, @Param("me") Resident me);
}
