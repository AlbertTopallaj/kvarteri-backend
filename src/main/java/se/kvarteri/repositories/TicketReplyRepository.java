package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.kvarteri.models.Ticket;
import se.kvarteri.models.TicketReply;

import java.util.List;
import java.util.UUID;

public interface TicketReplyRepository extends JpaRepository<TicketReply, UUID> {

    List<TicketReply> findByTicketOrderByCreatedAt(Ticket ticket);
}
