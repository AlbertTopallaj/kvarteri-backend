package se.kvarteri.services.email;

import lombok.extern.slf4j.Slf4j;

/** Fallback used when no email provider is configured (e.g. local dev): logs instead of sending. */
@Slf4j
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(String toEmail, String toName, String subject, String htmlBody) {
        log.info("[email disabled] would send to {} | subject: {}", toEmail, subject);
    }
}
