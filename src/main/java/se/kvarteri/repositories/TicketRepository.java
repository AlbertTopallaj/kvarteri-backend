package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import se.kvarteri.models.Association;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Ticket;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByCreatedByOrderByCreatedAtDesc(Resident createdBy);

    List<Ticket> findByCreatedByAssociationOrderByCreatedAtDesc(Association association);
}
