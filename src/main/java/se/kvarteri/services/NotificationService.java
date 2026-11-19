package se.kvarteri.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import se.kvarteri.models.Association;
import se.kvarteri.models.Notification;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Role;
import se.kvarteri.repositories.NotificationRepository;
import se.kvarteri.repositories.ResidentRepository;
import se.kvarteri.services.email.NotificationCreatedEvent;

/** Creates in-app notifications. Call inside the caller's transaction so they commit together. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifications;
    private final ResidentRepository residents;
    private final ApplicationEventPublisher events;

    public void notify(Resident to, String text) {
        Notification n = new Notification();
        n.setRecipient(to);
        n.setText(text.length() > 1000 ? text.substring(0, 1000) : text);
        notifications.save(n);
        // Emailed after the surrounding transaction commits; see EmailNotificationListener.
        events.publishEvent(new NotificationCreatedEvent(to.getEmail(), to.getName(), n.getText()));
    }

    public void notifyRole(Association association, Role role, String text) {
        residents.findByAssociationAndRoleAndEnabledTrue(association, role).forEach(r -> notify(r, text));
    }
}
