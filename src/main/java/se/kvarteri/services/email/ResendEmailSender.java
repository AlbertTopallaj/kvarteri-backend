package se.kvarteri.services.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Sends email through the Resend HTTP API (https://resend.com). Active only when an API key is set.
 * Failures are logged, never propagated: a missed email must not break the request that triggered it.
 */
@Slf4j
public class ResendEmailSender implements EmailSender {

    private static final String ENDPOINT = "https://api.resend.com/emails";

    private final RestClient client;
    private final String from;

    public ResendEmailSender(String apiKey, String from) {
        this.client = RestClient.builder()
                .baseUrl(ENDPOINT)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.from = from;
    }

    @Override
    public void send(String toEmail, String toName, String subject, String htmlBody) {
        try {
            client.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ResendRequest(from, List.of(toEmail), subject, htmlBody))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Sent email to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    /** Serialized to the JSON body Resend expects. */
    private record ResendRequest(String from, List<String> to, String subject, String html) {
    }
}
