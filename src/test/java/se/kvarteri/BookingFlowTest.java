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
import se.kvarteri.models.*;
import se.kvarteri.repositories.*;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BookingFlowTest {

    @Autowired WebApplicationContext ctx;
    @Autowired ResidentRepository residents;
    @Autowired CommunityRoomRepository rooms;
    @Autowired BookingRepository bookings;
    @Autowired TicketReplyRepository replies;
    @Autowired TicketRepository tickets;
    @Autowired MessageRepository messages;
    @Autowired NotificationRepository notificationRepo;
    @Autowired ParkingSpotRepository parkings;
    @Autowired SpotQueueEntryRepository spotQueue;
    @Autowired PasswordEncoder encoder;
    @Autowired se.kvarteri.repositories.AssociationRepository associations;
    se.kvarteri.models.Association assoc;
    MockMvc mvc;
    CommunityRoom room;

    @org.junit.jupiter.api.AfterEach
    void cleanUp() {
        bookings.deleteAll();
        replies.deleteAll();
        notificationRepo.deleteAll();
        tickets.deleteAll();
        messages.deleteAll();
        spotQueue.deleteAll();
        parkings.deleteAll();
        rooms.deleteAll();
        residents.deleteAll();
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        assoc = newAssociation();
        bookings.deleteAll();
        replies.deleteAll();
        notificationRepo.deleteAll();
        tickets.deleteAll();
        messages.deleteAll();
        spotQueue.deleteAll();
        parkings.deleteAll();
        rooms.deleteAll();
        residents.deleteAll();
        save("a@test.se", Role.RESIDENT);
        save("b@test.se", Role.RESIDENT);
        save("board@test.se", Role.BOARD_MEMBER);
        save("lock@test.se", Role.RESIDENT);
        room = new CommunityRoom();
        room.setName("Party room");
        room.setCapacity(20);
        room.setAssociation(assoc);
        room = rooms.save(room);
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

    private String period(int fromHours, int toHours) {
        LocalDateTime base = LocalDateTime.now().plusDays(2).withMinute(0).withSecond(0).withNano(0);
        return "{\"start\":\"" + base.plusHours(fromHours) + "\",\"end\":\"" + base.plusHours(toHours) + "\"}";
    }

    private org.springframework.test.web.servlet.ResultActions book(String user, String json) throws Exception {
        return mvc.perform(post("/api/communityrooms/" + room.getId() + "/bookings")
                .with(httpBasic(user, "password123")).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void overlappingBookingIsRejectedButAdjacentIsAllowed() throws Exception {
        book("a@test.se", period(10, 14)).andExpect(status().isCreated());
        book("b@test.se", period(12, 16)).andExpect(status().isConflict());
        book("b@test.se", period(14, 16)).andExpect(status().isCreated());
    }

    @Test
    void invalidPeriodsAreRejected() throws Exception {
        book("a@test.se", period(14, 10)).andExpect(status().isBadRequest());
        book("a@test.se", period(0, 20)).andExpect(status().isBadRequest());
        book("a@test.se", "{\"start\":\"2020-01-01T10:00:00\",\"end\":\"2020-01-01T12:00:00\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyOwnerOrStaffCanCancel() throws Exception {
        book("a@test.se", period(10, 12)).andExpect(status().isCreated());
        Booking b = bookings.findAll().get(0);
        mvc.perform(delete("/api/bookings/" + b.getId()).with(httpBasic("b@test.se", "password123")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/bookings/" + b.getId()).with(httpBasic("board@test.se", "password123")))
                .andExpect(status().isNoContent());
    }

    @Test
    void ticketThreadIsPrivateToCreatorAndStaff() throws Exception {
        mvc.perform(post("/api/tickets").with(httpBasic("a@test.se", "password123"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"subject\":\"s\",\"body\":\"b\"}"))
                .andExpect(status().isCreated());
        Ticket t = tickets.findAll().get(0);
        String url = "/api/tickets/" + t.getId() + "/replies";
        String body = "{\"body\":\"hello\"}";
        mvc.perform(post(url).with(httpBasic("b@test.se", "password123")).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isNotFound());
        mvc.perform(post(url).with(httpBasic("board@test.se", "password123")).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated());
        mvc.perform(get(url).with(httpBasic("a@test.se", "password123"))).andExpect(status().isOk());
        mvc.perform(patch("/api/tickets/" + t.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}").with(httpBasic("board@test.se", "password123")))
                .andExpect(status().isOk());
        mvc.perform(post(url).with(httpBasic("a@test.se", "password123")).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isConflict());
    }

    @Test
    void repeatedWrongPasswordsLockTheClientOut() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/api/messages").with(httpBasic("lock@test.se", "wrong")))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/messages").with(httpBasic("lock@test.se", "password123")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void corsAllowsConfiguredOriginOnly() throws Exception {
        mvc.perform(options("/api/me").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/me").header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void meReturnsRoleAndUnauthorizedHasNoBrowserDialog() throws Exception {
        mvc.perform(get("/api/me").with(httpBasic("board@test.se", "password123")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.role").value("BOARD_MEMBER"));
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .doesNotExist("WWW-Authenticate"));
    }
}
