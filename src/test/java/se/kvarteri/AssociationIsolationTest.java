package se.kvarteri;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import se.kvarteri.models.*;
import se.kvarteri.repositories.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Users only see their own association; board members moderate (warn/remove) within it. */
@SpringBootTest
class AssociationIsolationTest {

    @Autowired WebApplicationContext ctx;
    @Autowired AssociationRepository associations;
    @Autowired ResidentRepository residents;
    @Autowired CommunityRoomRepository rooms;
    @Autowired LaundryRepository laundries;
    @Autowired ParkingSpotRepository spots;
    @Autowired SpotQueueEntryRepository queue;
    @Autowired BookingRepository bookings;
    @Autowired TicketReplyRepository replies;
    @Autowired TicketRepository tickets;
    @Autowired MessageRepository messages;
    @Autowired NotificationRepository notificationRepo;
    @Autowired WarningRepository warnings;
    @Autowired PasswordEncoder encoder;
    MockMvc mvc;
    Association a, b;
    Resident residentA, residentB, boardA, boardB, adminA;
    CommunityRoom roomA;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        cleanUp();
        a = association("Forening A");
        b = association("Forening B");
        residentA = user("resa@test.se", Role.RESIDENT, a);
        residentB = user("resb@test.se", Role.RESIDENT, b);
        boardA = user("boarda@test.se", Role.BOARD_MEMBER, a);
        boardB = user("boardb@test.se", Role.BOARD_MEMBER, b);
        adminA = user("admin@test.se", Role.ADMIN, a);
        roomA = new CommunityRoom();
        roomA.setName("Room A");
        roomA.setCapacity(10);
        roomA.setAssociation(a);
        roomA = rooms.save(roomA);
    }

    @AfterEach
    void cleanUp() {
        bookings.deleteAll();
        replies.deleteAll();
        warnings.deleteAll();
        notificationRepo.deleteAll();
        tickets.deleteAll();
        messages.deleteAll();
        queue.deleteAll();
        spots.deleteAll();
        rooms.deleteAll();
        laundries.deleteAll();
        residents.deleteAll();
        associations.deleteAll();
    }

    private Association association(String name) {
        Association x = new Association();
        x.setName(name);
        return associations.save(x);
    }

    private Resident user(String email, Role role, Association assoc) {
        Resident r = new Resident();
        r.setEmail(email);
        r.setName(email);
        r.setPasswordHash(encoder.encode("password123"));
        r.setRole(role);
        r.setAssociation(assoc);
        return residents.save(r);
    }

    private ResultActions as(String user, MockHttpServletRequestBuilder req) throws Exception {
        return mvc.perform(req.with(httpBasic(user, "password123")));
    }

    private static String json(String s) {
        return s;
    }

    // ---- isolation

    @Test
    void resourcesAreScopedToTheAssociation() throws Exception {
        as("resa@test.se", get("/api/communityrooms")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        as("resb@test.se", get("/api/communityrooms")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        String period = "{\"start\":\"" + java.time.LocalDateTime.now().plusDays(2).withMinute(0).withSecond(0).withNano(0)
                + "\",\"end\":\"" + java.time.LocalDateTime.now().plusDays(2).plusHours(2).withMinute(0).withSecond(0).withNano(0) + "\"}";
        as("resb@test.se", post("/api/communityrooms/" + roomA.getId() + "/bookings")
                .contentType(MediaType.APPLICATION_JSON).content(period)).andExpect(status().isNotFound());
        as("resb@test.se", get("/api/communityrooms/" + roomA.getId() + "/bookings")).andExpect(status().isNotFound());
        as("resa@test.se", post("/api/communityrooms/" + roomA.getId() + "/bookings")
                .contentType(MediaType.APPLICATION_JSON).content(period)).andExpect(status().isCreated());
    }

    @Test
    void foreignBookingCannotBeCancelledByOtherAssociationsBoard() throws Exception {
        String period = "{\"start\":\"" + java.time.LocalDateTime.now().plusDays(2).withMinute(0).withSecond(0).withNano(0)
                + "\",\"end\":\"" + java.time.LocalDateTime.now().plusDays(2).plusHours(2).withMinute(0).withSecond(0).withNano(0) + "\"}";
        String res = as("resa@test.se", post("/api/communityrooms/" + roomA.getId() + "/bookings")
                .contentType(MediaType.APPLICATION_JSON).content(period)).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(res, "$.id");
        as("boardb@test.se", delete("/api/bookings/" + id)).andExpect(status().isNotFound());
        as("boarda@test.se", delete("/api/bookings/" + id)).andExpect(status().isNoContent());
    }

    @Test
    void messagesAreScopedAndCannotTargetOtherAssociation() throws Exception {
        as("boarda@test.se", post("/api/messages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Hej A\",\"body\":\"b\"}")).andExpect(status().isCreated());
        as("resa@test.se", get("/api/messages")).andExpect(jsonPath("$.length()").value(1));
        as("resb@test.se", get("/api/messages")).andExpect(jsonPath("$.length()").value(0));
        as("boardb@test.se", get("/api/messages")).andExpect(jsonPath("$.length()").value(0));
        as("resb@test.se", get("/api/notifications")).andExpect(jsonPath("$.length()").value(0));

        as("boarda@test.se", post("/api/messages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"t\",\"body\":\"b\",\"recipientId\":\"" + residentB.getId() + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ticketsAreScopedToTheAssociation() throws Exception {
        String res = as("resa@test.se", post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"Trasig dörr\"}")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(res, "$.id");

        as("boarda@test.se", get("/api/tickets")).andExpect(jsonPath("$.length()").value(1));
        as("boardb@test.se", get("/api/tickets")).andExpect(jsonPath("$.length()").value(0));
        as("boardb@test.se", patch("/api/tickets/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}")).andExpect(status().isNotFound());
        as("boardb@test.se", get("/api/tickets/" + id + "/replies")).andExpect(status().isNotFound());
        as("boardb@test.se", post("/api/tickets/" + id + "/replies").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"hej\"}")).andExpect(status().isNotFound());
        as("boarda@test.se", patch("/api/tickets/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}")).andExpect(status().isOk());

        // Only the board of the ticket's own association is notified.
        as("boardb@test.se", get("/api/notifications")).andExpect(jsonPath("$.length()").value(0));
        as("boarda@test.se", get("/api/notifications")).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void parkingSpotsAreScopedToTheAssociation() throws Exception {
        String res = as("boarda@test.se", post("/api/parking-spots").contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"PARKING\",\"name\":\"P1\",\"price\":500}")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(res, "$.id");
        // Same name is allowed in another association.
        as("boardb@test.se", post("/api/parking-spots").contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"PARKING\",\"name\":\"P1\",\"price\":500}")).andExpect(status().isCreated());

        as("resa@test.se", get("/api/parking-spots")).andExpect(jsonPath("$.length()").value(1));
        as("resb@test.se", post("/api/parking-spots/" + id + "/queue")).andExpect(status().isNotFound());
        as("boardb@test.se", post("/api/parking-spots/" + id + "/assign").contentType(MediaType.APPLICATION_JSON)
                .content("{\"residentId\":\"" + residentB.getId() + "\"}")).andExpect(status().isNotFound());
        as("boardb@test.se", delete("/api/parking-spots/" + id)).andExpect(status().isNotFound());
        as("boarda@test.se", post("/api/parking-spots/" + id + "/assign").contentType(MediaType.APPLICATION_JSON)
                .content("{\"residentId\":\"" + residentB.getId() + "\"}")).andExpect(status().isNotFound());
        as("boarda@test.se", post("/api/parking-spots/" + id + "/assign").contentType(MediaType.APPLICATION_JSON)
                .content("{\"residentId\":\"" + residentA.getId() + "\"}")).andExpect(status().isOk());
    }

    @Test
    void residentListOnlyShowsOwnAssociation() throws Exception {
        as("boarda@test.se", get("/api/residents")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='resb@test.se')]").isEmpty())
                .andExpect(jsonPath("$[?(@.name=='resa@test.se')]").isNotEmpty());
    }

    // ---- moderation

    @Test
    void boardCanWarnResidentAndResidentSeesIt() throws Exception {
        as("boarda@test.se", post("/api/residents/" + residentA.getId() + "/warnings")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Buller efter 22\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.reason").value("Buller efter 22"));
        as("resa@test.se", get("/api/me/warnings")).andExpect(jsonPath("$.length()").value(1));
        as("resa@test.se", get("/api/notifications")).andExpect(jsonPath("$.length()").value(1));
        as("boarda@test.se", get("/api/residents/" + residentA.getId() + "/warnings"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void residentCannotModerate() throws Exception {
        as("resa@test.se", post("/api/residents/" + residentA.getId() + "/warnings")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}")).andExpect(status().isForbidden());
        as("resa@test.se", delete("/api/residents/" + residentA.getId())).andExpect(status().isForbidden());
    }

    @Test
    void boardCannotModerateOtherAssociationsOrPrivilegedUsersOrSelf() throws Exception {
        as("boarda@test.se", post("/api/residents/" + residentB.getId() + "/warnings")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}")).andExpect(status().isNotFound());
        as("boarda@test.se", delete("/api/residents/" + residentB.getId())).andExpect(status().isNotFound());
        assertFalse(warnings.findAll().iterator().hasNext());

        // Another board member / admin in the same association: not allowed for board.
        user("boarda2@test.se", Role.BOARD_MEMBER, a);
        Resident other = residents.findByEmailIgnoreCase("boarda2@test.se").orElseThrow();
        as("boarda@test.se", delete("/api/residents/" + other.getId())).andExpect(status().isForbidden());
        as("boarda@test.se", delete("/api/residents/" + adminA.getId())).andExpect(status().isForbidden());
        as("boarda@test.se", delete("/api/residents/" + boardA.getId())).andExpect(status().isConflict());
    }

    @Test
    void boardCanRemoveResidentInOwnAssociation() throws Exception {
        as("boarda@test.se", post("/api/residents/" + residentA.getId() + "/warnings")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}")).andExpect(status().isCreated());
        as("boarda@test.se", delete("/api/residents/" + residentA.getId())).andExpect(status().isNoContent());
        as("resa@test.se", get("/api/me")).andExpect(status().isUnauthorized());
        as("boarda@test.se", delete("/api/residents/" + residentA.getId())).andExpect(status().isNotFound());
    }

    @Test
    void adminCanModerateAnyAssociation() throws Exception {
        as("admin@test.se", post("/api/residents/" + residentB.getId() + "/warnings")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}")).andExpect(status().isCreated());
        as("admin@test.se", delete("/api/residents/" + boardB.getId())).andExpect(status().isNoContent());
    }

    // ---- admin: associations

    @Test
    void adminCreatesAssociationAndAccountInIt() throws Exception {
        as("boarda@test.se", post("/api/admin/associations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Forening C\"}")).andExpect(status().isForbidden());
        String res = as("admin@test.se", post("/api/admin/associations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Forening C\"}")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(res, "$.id");
        as("admin@test.se", post("/api/admin/associations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"forening c\"}")).andExpect(status().isConflict());

        as("admin@test.se", post("/api/admin/residents").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"c@test.se\",\"password\":\"longpassword1\",\"name\":\"C\",\"role\":\"RESIDENT\","
                        + "\"associationId\":\"" + id + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.association.name").value("Forening C"));
        mvc.perform(get("/api/me").with(httpBasic("c@test.se", "longpassword1")))
                .andExpect(jsonPath("$.association.name").value("Forening C"));
        mvc.perform(get("/api/communityrooms").with(httpBasic("c@test.se", "longpassword1")))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
