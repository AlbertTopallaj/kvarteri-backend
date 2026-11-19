package se.kvarteri.services.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Wires the active {@link EmailSender}: Resend when an API key is present, otherwise a logging no-op
 * so the app runs without email configured. {@code @EnableAsync} lets emails be sent off the request thread.
 */
@Configuration
@EnableAsync
@Slf4j
public class EmailConfig {

    @Bean
    EmailSender emailSender(@Value("${kvarteri.email.resend.api-key:}") String apiKey,
                            @Value("${kvarteri.email.from:}") String from) {
        if (apiKey.isBlank() || from.isBlank()) {
            log.info("Email disabled: set RESEND_API_KEY and EMAIL_FROM to enable outgoing email");
            return new LoggingEmailSender();
        }
        log.info("Email enabled via Resend (from {})", from);
        return new ResendEmailSender(apiKey, from);
    }
}
