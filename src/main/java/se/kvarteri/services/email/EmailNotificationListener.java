package se.kvarteri.services.email;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

/**
 * Turns a committed notification into an email. Runs only after the surrounding transaction commits
 * (so a rolled-back notification is never emailed) and off the request thread.
 */
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private static final int SUBJECT_MAX = 120;

    private final EmailSender emailSender;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotification(NotificationCreatedEvent event) {
        emailSender.send(event.recipientEmail(), event.recipientName(), subject(event.text()), html(event));
    }

    private static String subject(String text) {
        // Resend rejects newlines in the subject, so collapse all whitespace to single spaces.
        String s = text.replaceAll("\\s+", " ").strip();
        return s.length() > SUBJECT_MAX ? s.substring(0, SUBJECT_MAX - 1).strip() + "…" : s;
    }

    private static String html(NotificationCreatedEvent event) {
        String greeting = Objects.toString(event.recipientName(), "").isBlank()
                ? "Hej," : "Hej " + escape(event.recipientName()) + ",";
        return """
                <div style="font-family: system-ui, sans-serif; color: #111827; max-width: 480px;">
                  %s
                  <p>%s</p>
                  <p>%s</p>
                  <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;">
                  <p style="color: #6b7280; font-size: 13px;">
                    Det här är ett automatiskt meddelande från Kvarteri. Logga in för att se mer.
                  </p>
                </div>
                """.formatted(logo(), greeting, escape(event.text()));
    }

    /**
     * Email-safe Kvarteri logo: a dark rounded tile with four light "windows", built from tables and
     * inline styles (SVG is unreliable in email clients), plus the wordmark. Mirrors the in-app SVG mark.
     */
    private static String logo() {
        return """
                <table role="presentation" cellpadding="0" cellspacing="0" style="border-collapse: collapse; margin-bottom: 24px;">
                  <tr>
                    <td style="vertical-align: middle;">
                      <table role="presentation" cellpadding="0" cellspacing="0" style="background: #111827; border-radius: 9px;">
                        <tr><td style="padding: 8px;">
                          <table role="presentation" cellpadding="0" cellspacing="0" style="border-collapse: separate; border-spacing: 3px;">
                            <tr>
                              <td style="width: 7px; height: 7px; background: #ffffff; border-radius: 2px;"></td>
                              <td style="width: 7px; height: 7px; background: #ffffff; border-radius: 2px;"></td>
                            </tr>
                            <tr>
                              <td style="width: 7px; height: 7px; background: #ffffff; border-radius: 2px;"></td>
                              <td style="width: 7px; height: 7px; background: #ffffff; border-radius: 2px;"></td>
                            </tr>
                          </table>
                        </td></tr>
                      </table>
                    </td>
                    <td style="padding-left: 10px; vertical-align: middle; font-family: system-ui, sans-serif; font-size: 20px; font-weight: 600; letter-spacing: -0.02em; color: #111827;">
                      Kvarteri
                    </td>
                  </tr>
                </table>
                """;
    }

    /** Minimal HTML escaping for user-supplied notification text. */
    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
