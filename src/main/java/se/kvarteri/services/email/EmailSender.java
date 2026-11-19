package se.kvarteri.services.email;

/** Sends a single transactional email. Implementations must not throw to the caller. */
public interface EmailSender {

    void send(String toEmail, String toName, String subject, String htmlBody);
}
