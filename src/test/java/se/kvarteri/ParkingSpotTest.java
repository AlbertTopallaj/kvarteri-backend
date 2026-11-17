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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Role;
import se.kvarteri.repositories.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ParkingSpotTest {

    @Autowired WebApplicationContext ctx;
    @Autowired ResidentRepository residents;
    @Autowired SpotQueueEntryRepository queue;
    @Autowired ParkingSpotRepository spots;
    @Autowired MessageRepository messages;
    @Autowired TicketRepository tickets;
    @Autowired PasswordEncoder encoder;
    MockMvc mvc;
    Resident a, b;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        cleanUp();
        a = save("a@test.se", "Anna", Role.RESIDENT);
        b = save("b@test.se", "Bo", Role.RESIDENT);
        save("board@test.se", "Styrelse", Role.BOARD_MEMBER);
    }

    @AfterEach
    void cleanUp() {
        queue.deleteAll();
        spots.deleteAll();
        tickets.deleteAll();
        messages.deleteAll();
        residents.deleteAll();
    }

    private Resident save(String email, String name, Role role) {
        Resident r = new Resident();
        r.setEmail(email);
        r.setName(name);
        r.setApartmentNumber("1101");
        r.setPasswordHash(encoder.encode("password123"));
        r.setRole(role);
        return residents.save(r);
    }

    private ResultActions as(String user, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req)
            throws Exception {
        return mvc.perform(req.with(httpBasic(user, "password123")));
    }

    private String createSpot(String kind, String name) throws Exception {
        String res = as("board@test.se", post("/api/parking-spots").contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"" + kind + "\",\"name\":\"" + name + "\",\"price\":500}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.id");
    }

    private String assignBody(Resident r) {
        return "{\"residentId\":\"" + r.getId() + "\"}";
    }

    @Test
    void onlyStaffManageSpots() throws Exception {
        as("a@test.se", post("/api/parking-spots").contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"PARKING\",\"name\":\"P1\",\"price\":500}")).andExpect(status().isForbidden());
        String id = createSpot("PARKING", "P1");
        as("a@test.se", delete("/api/parking-spots/" + id)).andExpect(status().isForbidden());
        as("a@test.se", patch("/api/parking-spots/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":1}")).andExpect(status().isForbidden());
        as("board@test.se", patch("/api/parking-spots/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":650}")).andExpect(status().isOk()).andExpect(jsonPath("$.price").value(650));
        as("board@test.se", post("/api/parking-spots").contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"PARKING\",\"name\":\"P1\",\"price\":1}")).andExpect(status().isConflict());
    }

    @Test
    void queueKeepsOrderAndRejectsDuplicates() throws Exception {
        String id = createSpot("GARAGE", "G1");
        as("a@test.se", post("/api/parking-spots/" + id + "/queue")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.myQueuePosition").value(1));
        as("b@test.se", post("/api/parking-spots/" + id + "/queue")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.myQueuePosition").value(2))
                .andExpect(jsonPath("$.queueLength").value(2));
        as("b@test.se", post("/api/parking-spots/" + id + "/queue")).andExpect(status().isConflict());

        as("b@test.se", get("/api/parking-spots")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("GARAGE"))
                .andExpect(jsonPath("$[0].queue[0].name").value("Anna"))
                .andExpect(jsonPath("$[0].myQueuePosition").value(2))
                .andExpect(jsonPath("$[0].tenant").doesNotExist());

        as("a@test.se", delete("/api/parking-spots/" + id + "/queue")).andExpect(status().isOk())
                .andExpect(jsonPath("$.myQueuePosition").doesNotExist());
        as("a@test.se", delete("/api/parking-spots/" + id + "/queue")).andExpect(status().isNotFound());
        as("b@test.se", get("/api/parking-spots")).andExpect(jsonPath("$[0].myQueuePosition").value(1));
    }

    @Test
    void assignRemovesFromQueueAndNotifiesThenTerminateNotifiesNext() throws Exception {
        String id = createSpot("PARKING", "P7");
        as("a@test.se", post("/api/parking-spots/" + id + "/queue")).andExpect(status().isCreated());
        as("b@test.se", post("/api/parking-spots/" + id + "/queue")).andExpect(status().isCreated());

        as("a@test.se", post("/api/parking-spots/" + id + "/assign").contentType(MediaType.APPLICATION_JSON)
                .content(assignBody(a))).andExpect(status().isForbidden());
        as("board@test.se", post("/api/parking-spots/" + id + "/assign").contentType(MediaType.APPLICATION_JSON)
                .content(assignBody(a))).andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant.name").value("Anna"))
                .andExpect(jsonPath("$.tenant.apartmentNumber").value("1101"))
                .andExpect(jsonPath("$.queueLength").value(1));
        as("board@test.se", post("/api/parking-spots/" + id + "/assign").contentType(MediaType.APPLICATION_JSON)
                .content(assignBody(b))).andExpect(status().isConflict());

        // tenant can neither queue for the spot nor see it as someone else's
        as("a@test.se", post("/api/parking-spots/" + id + "/queue")).andExpect(status().isConflict());
        as("a@test.se", get("/api/parking-spots")).andExpect(jsonPath("$[0].mine").value(true));
        as("a@test.se", get("/api/messages")).andExpect(jsonPath("$[0].title").value("Du har tilldelats parkeringsplats P7"));

        as("board@test.se", delete("/api/parking-spots/" + id)).andExpect(status().isConflict());
        as("board@test.se", post("/api/parking-spots/" + id + "/terminate")).andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant").doesNotExist());
        as("board@test.se", post("/api/parking-spots/" + id + "/terminate")).andExpect(status().isConflict());

        String bMessages = as("b@test.se", get("/api/messages")).andReturn().getResponse().getContentAsString();
        assertEquals("parkeringsplats P7 är nu ledig", ((String) JsonPath.read(bMessages, "$[0].title")));
        as("a@test.se", get("/api/messages")).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void deleteFreeSpotRemovesItsQueue() throws Exception {
        String id = createSpot("PARKING", "P9");
        as("a@test.se", post("/api/parking-spots/" + id + "/queue")).andExpect(status().isCreated());
        as("board@test.se", delete("/api/parking-spots/" + id)).andExpect(status().isNoContent());
        as("a@test.se", get("/api/parking-spots")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void residentListIsStaffOnlyAndHidesEmail() throws Exception {
        as("a@test.se", get("/api/residents")).andExpect(status().isForbidden());
        as("board@test.se", get("/api/residents")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Anna"))
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    void messagesAndTicketsListWithRelatedNames() throws Exception {
        as("board@test.se", post("/api/messages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Hej\",\"body\":\"Alla\"}")).andExpect(status().isCreated());
        as("a@test.se", get("/api/messages")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].author").value("Styrelse"));
        as("a@test.se", post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"S\",\"body\":\"B\"}")).andExpect(status().isCreated());
        as("a@test.se", get("/api/tickets")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].createdBy").value("Anna"));
        as("board@test.se", get("/api/tickets")).andExpect(jsonPath("$[0].createdBy").value("Anna"));
    }
}
