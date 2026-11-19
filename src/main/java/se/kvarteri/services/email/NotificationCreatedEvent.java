package se.kvarteri.services.email;

/** Published when an in-app notification is saved, so an email can be sent after the transaction commits. */
public record NotificationCreatedEvent(String recipientEmail, String recipientName, String text) {
}
