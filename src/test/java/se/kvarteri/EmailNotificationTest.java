package se.kvarteri;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import se.kvarteri.models.Association;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Role;
import se.kvarteri.repositories.*;
import se.kvarteri.services.email.EmailSender;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies that creating an in-app notification also sends an email, after the transaction commits. */
@SpringBootTest
class EmailNotificationTest {

    /** Captures emails in-memory instead of calling Resend. */
    record SentEmail(String to, String subject) {
    }

    @TestConfiguration
    static class RecordingEmailConfig {
        static final List<SentEmail> sent = new CopyOnWriteArrayList<>();

        @Bean
        @Primary
        EmailSender recordingEmailSender() {
            return (toEmail, toName, subject, htmlBody) -> sent.add(new SentEmail(toEmail, subject));
        }
    }

    @Autowired WebApplicationContext ctx;
    @Autowired ResidentRepository residents;
    @Autowired WarningRepository warnings;
    @Autowired NotificationRepository notificationRepo;
    @Autowired AssociationRepository associations;
    @Autowired PasswordEncoder encoder;

    MockMvc mvc;
    Association assoc;
    Resident resident, board;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        RecordingEmailConfig.sent.clear();
        assoc = associations.save(newAssociation());
        resident = save("resident@test.se", "Rut", Role.RESIDENT);
        board = save("board@test.se", "Bengt", Role.BOARD_MEMBER);
    }

    @AfterEach
    void cleanUp() {
        warnings.deleteAll();
        notificationRepo.deleteAll();
        residents.deleteAll();
        RecordingEmailConfig.sent.clear();
    }

    @Test
    void warningSendsEmailToTheResident() throws Exception {
        mvc.perform(post("/api/residents/" + resident.getId() + "/warnings")
                        .with(httpBasic("board@test.se", "password12"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Fel soprum\"}"))
                .andExpect(status().isCreated());

        SentEmail email = awaitFirstEmail();
        assertEquals("resident@test.se", email.to());
        assertTrue(email.subject().toLowerCase().contains("varning"),
                "subject should mention the warning, was: " + email.subject());
    }

    /** The email is sent asynchronously after commit, so poll briefly for it. */
    private SentEmail awaitFirstEmail() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (!RecordingEmailConfig.sent.isEmpty()) {
                return RecordingEmailConfig.sent.get(0);
            }
            Thread.sleep(25);
        }
        throw new AssertionError("No email was sent within the timeout");
    }

    private Association newAssociation() {
        Association a = new Association();
        a.setName("Testförening " + java.util.UUID.randomUUID());
        return a;
    }

    private Resident save(String email, String name, Role role) {
        Resident r = new Resident();
        r.setEmail(email);
        r.setName(name);
        r.setRole(role);
        r.setPasswordHash(encoder.encode("password12"));
        r.setAssociation(assoc);
        return residents.save(r);
    }
}
