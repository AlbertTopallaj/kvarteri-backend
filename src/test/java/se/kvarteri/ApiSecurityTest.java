package se.kvarteri;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Role;
import se.kvarteri.repositories.ResidentRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ApiSecurityTest {

    @Autowired WebApplicationContext ctx;
    @Autowired ResidentRepository residents;
    @Autowired se.kvarteri.repositories.MessageRepository messages;
    @Autowired se.kvarteri.repositories.NotificationRepository notificationRepo;
    @Autowired PasswordEncoder encoder;
    @Autowired se.kvarteri.repositories.AssociationRepository associations;
    se.kvarteri.models.Association assoc;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        assoc = newAssociation();
        notificationRepo.deleteAll();
        messages.deleteAll();
        residents.deleteAll();
        save("res@test.se", Role.RESIDENT);
        save("board@test.se", Role.BOARD_MEMBER);
        save("admin@test.se", Role.ADMIN);
    }

    private se.kvarteri.models.Association newAssociation() {
        se.kvarteri.models.Association a = new se.kvarteri.models.Association();
        a.setName("Test-" + java.util.UUID.randomUUID());
        return associations.save(a);
    }

    private void save(String email, Role role) {
        Resident r = new Resident();
        r.setEmail(email);
        r.setName(email);
        r.setPasswordHash(encoder.encode("password123"));
        r.setRole(role);
        r.setAssociation(assoc);
        residents.save(r);
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/messages")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(get("/api/messages").with(httpBasic("res@test.se", "nope"))).andExpect(status().isUnauthorized());
    }

    @Test
    void residentCanReadButNotSendMessages() throws Exception {
        mvc.perform(get("/api/messages").with(httpBasic("res@test.se", "password123"))).andExpect(status().isOk());
        mvc.perform(post("/api/messages").with(httpBasic("res@test.se", "password123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"t\",\"body\":\"b\"}")).andExpect(status().isForbidden());
    }

    @Test
    void boardCanSendMessage() throws Exception {
        mvc.perform(post("/api/messages").with(httpBasic("board@test.se", "password123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"t\",\"body\":\"b\"}")).andExpect(status().isCreated());
    }

    @Test
    void onlyAdminCreatesResidents() throws Exception {
        String body = "{\"email\":\"new@test.se\",\"password\":\"longpassword1\",\"name\":\"N\",\"role\":\"RESIDENT\"}";
        mvc.perform(post("/api/admin/residents").with(httpBasic("board@test.se", "password123"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/residents").with(httpBasic("admin@test.se", "password123"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
    }

    @Test
    void residentCannotCloseTicket() throws Exception {
        mvc.perform(patch("/api/tickets/" + java.util.UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}")
                .with(httpBasic("res@test.se", "password123"))).andExpect(status().isForbidden());
    }
}
