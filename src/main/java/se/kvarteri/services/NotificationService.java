package se.kvarteri.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.kvarteri.models.Association;
import se.kvarteri.models.Notification;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Role;
import se.kvarteri.repositories.NotificationRepository;
import se.kvarteri.repositories.ResidentRepository;

/** Creates in-app notifications. Call inside the caller's transaction so they commit together. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifications;
    private final ResidentRepository residents;

    public void notify(Resident to, String text) {
        Notification n = new Notification();
        n.setRecipient(to);
        n.setText(text.length() > 1000 ? text.substring(0, 1000) : text);
        notifications.save(n);
    }

    public void notifyRole(Association association, Role role, String text) {
        residents.findByAssociationAndRoleAndEnabledTrue(association, role).forEach(r -> notify(r, text));
    }
}
