package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import se.kvarteri.models.Notification;
import se.kvarteri.models.Resident;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findTop50ByRecipientOrderByCreatedAtDesc(Resident recipient);

    @Modifying
    @Query("update Notification n set n.seen = true where n.recipient = ?1 and n.seen = false")
    void markAllSeen(Resident recipient);

    @Modifying
    @Query("delete from Notification n where n.recipient = ?1")
    void deleteByRecipient(Resident recipient);
}
