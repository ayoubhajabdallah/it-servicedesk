package dev.ayoub.servicedesk;

import com.jayway.jsonpath.JsonPath;
import dev.ayoub.servicedesk.domain.*;
import dev.ayoub.servicedesk.repository.TicketRepository;
import dev.ayoub.servicedesk.repository.UserRepository;
import dev.ayoub.servicedesk.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:servicedesk-http-test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "jwt.secret=local-demo-only-0123456789abcdef"
})
class ServiceDeskHttpTest {
    private static final String PASSWORD = "StrongPass123!";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static String passwordHash;

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository users;
    @Autowired private TicketRepository tickets;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mvc;
    private User employee;
    private User technician;
    private User admin;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        tickets.deleteAll();
        users.deleteAll();
        if (passwordHash == null) {
            passwordHash = passwordEncoder.encode(PASSWORD);
        }
        employee = createUser("employee@example.com", UserRole.EMPLOYEE);
        technician = createUser("technician@example.com", UserRole.TECHNICIAN);
        admin = createUser("admin@example.com", UserRole.ADMIN);
    }

    @Test
    void registrationNormalizesEmailAndStoresEncodedEmployeePassword() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("name", "New user",
                                "email", "  NEW.User@Example.COM  ", "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.user@example.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.password").doesNotExist());
        User saved = users.findByEmail("new.user@example.com").orElseThrow();
        assertNotEquals(PASSWORD, saved.getPassword());
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getPassword()));
    }

    @Test
    void normalizedDuplicateRegistrationReturnsConflict() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("name", "Duplicate",
                                "email", "  EMPLOYEE@Example.COM  ", "password", PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already registered"));
        assertEquals(3, users.count());
    }

    @Test
    void normalizedLoginReturnsRealTokenAcceptedByProtectedEndpoint() throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("email", "  EMPLOYEE@Example.COM  ",
                                "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value(employee.getEmail()))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.token");
        assertEquals(employee.getEmail(), jwtService.extractEmail(token));
        mvc.perform(get("/api/tickets").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"employee@example.com, WrongPass123!", "unknown@example.com, StrongPass123!"})
    void invalidLoginCredentialsReturnUnauthorized(String email, String password) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"register", "login"})
    void invalidEmailReturnsBadRequest(String endpoint) throws Exception {
        mvc.perform(post("/api/auth/" + endpoint).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("name", "User", "email", "not-an-email",
                                "password", PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("email")));
    }

    @Test
    void validTicketCreationUsesServerOwnedFields() throws Exception {
        String response = createTicket(validTicket())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdById").value(employee.getId()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedToId").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        Ticket saved = tickets.findById(ticketId(response)).orElseThrow();
        assertEquals("Printer offline", saved.getTitle());
        assertEquals(employee.getId(), saved.getCreatedBy().getId());
        assertEquals(TicketStatus.OPEN, saved.getStatus());
        assertNull(saved.getAssignedTo());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void suppliedInternalFieldsCannotControlCreatedTicket() throws Exception {
        Map<String, Object> body = validTicket();
        body.put("id", 999999);
        body.put("createdBy", Map.of("id", admin.getId()));
        body.put("assignedTo", Map.of("id", technician.getId()));
        body.put("status", "CLOSED");
        body.put("createdAt", "2000-01-01T00:00:00");
        body.put("updatedAt", "2000-01-01T00:00:00");
        String response = createTicket(body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdById").value(employee.getId()))
                .andExpect(jsonPath("$.assignedToId").value(nullValue()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        Ticket saved = tickets.findById(ticketId(response)).orElseThrow();
        assertNotEquals(999999L, saved.getId());
        assertNotEquals(2000, saved.getCreatedAt().getYear());
        assertNotEquals(2000, saved.getUpdatedAt().getYear());
        assertEquals(employee.getId(), saved.getCreatedBy().getId());
        assertNull(saved.getAssignedTo());
    }

    static Stream<Map<String, Object>> invalidTickets() {
        return Stream.of(
                changedTicket("title", " "),
                changedTicket("title", null),
                changedTicket("description", " "),
                changedTicket("description", null),
                changedTicket("category", null),
                changedTicket("priority", null),
                changedTicket("title", "x".repeat(151)),
                changedTicket("description", "x".repeat(3001)),
                changedTicket("category", "INVALID"),
                changedTicket("priority", "INVALID"));
    }

    @ParameterizedTest
    @MethodSource("invalidTickets")
    void invalidTicketReturnsBadRequestWithoutSaving(Map<String, Object> body) throws Exception {
        createTicket(body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
        assertEquals(0, tickets.count());
    }

    @Test
    void maximumTicketLengthsAreAccepted() throws Exception {
        Map<String, Object> body = validTicket();
        body.put("title", "x".repeat(150));
        body.put("description", "x".repeat(3000));
        createTicket(body).andExpect(status().isCreated());
    }

    @Test
    void missingTicketReturnsNotFound() throws Exception {
        mvc.perform(get("/api/tickets/{id}", 999999).header("Authorization", bearer(employee)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Ticket not found: 999999"));
    }

    @Test
    void assigningMissingUserReturnsNotFound() throws Exception {
        Ticket ticket = persistedTicket();
        mvc.perform(patch("/api/tickets/{id}/assign/{userId}", ticket.getId(), 999999)
                        .header("Authorization", bearer(technician)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: 999999"));
        assertNull(tickets.findById(ticket.getId()).orElseThrow().getAssignedTo());
    }

    @Test
    void assigningNonTechnicianReturnsBadRequest() throws Exception {
        Ticket ticket = persistedTicket();
        mvc.perform(patch("/api/tickets/{id}/assign/{userId}", ticket.getId(), employee.getId())
                        .header("Authorization", bearer(technician)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Selected user is not a technician"));
        assertNull(tickets.findById(ticket.getId()).orElseThrow().getAssignedTo());
    }

    @Test
    void technicianCanAssignTicket() throws Exception {
        Ticket ticket = persistedTicket();
        mvc.perform(patch("/api/tickets/{id}/assign/{userId}", ticket.getId(), technician.getId())
                        .header("Authorization", bearer(technician)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToId").value(technician.getId()));
        assertEquals(technician.getId(), tickets.findById(ticket.getId()).orElseThrow().getAssignedTo().getId());
    }

    @Test
    void unauthenticatedProtectedRequestReturnsUnauthorized() throws Exception {
        mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedBearerTokenReturnsUnauthorized() throws Exception {
        String token = jwtService.generateToken(employee);
        int start = token.lastIndexOf('.') + 1;
        String tampered = token.substring(0, start) + (token.charAt(start) == 'A' ? 'B' : 'A')
                + token.substring(start + 1);
        mvc.perform(get("/api/tickets").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"status", "assign", "delete"})
    void employeeCannotMutateExistingTicket(String operation) throws Exception {
        Ticket ticket = persistedTicket();
        var request = switch (operation) {
            case "status" -> patch("/api/tickets/{id}/status", ticket.getId()).param("status", "CLOSED");
            case "assign" -> patch("/api/tickets/{id}/assign/{userId}", ticket.getId(), technician.getId());
            default -> delete("/api/tickets/{id}", ticket.getId());
        };
        mvc.perform(request.header("Authorization", bearer(employee))).andExpect(status().isForbidden());
        Ticket unchanged = tickets.findById(ticket.getId()).orElseThrow();
        assertEquals(TicketStatus.OPEN, unchanged.getStatus());
        assertNull(unchanged.getAssignedTo());
    }

    @Test
    void technicianCannotDeleteTicket() throws Exception {
        Ticket ticket = persistedTicket();
        mvc.perform(delete("/api/tickets/{id}", ticket.getId()).header("Authorization", bearer(technician)))
                .andExpect(status().isForbidden());
        assertTrue(tickets.existsById(ticket.getId()));
    }

    @Test
    void adminCanDeleteTicket() throws Exception {
        Ticket ticket = persistedTicket();
        mvc.perform(delete("/api/tickets/{id}", ticket.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        assertFalse(tickets.existsById(ticket.getId()));
    }

    private User createUser(String email, UserRole role) {
        return users.save(User.builder().name(role.name()).email(email).password(passwordHash).role(role).build());
    }

    private Ticket persistedTicket() {
        return tickets.save(Ticket.builder().title("Printer offline").description("Printer is unreachable")
                .category(TicketCategory.HARDWARE).priority(TicketPriority.MEDIUM).createdBy(employee).build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private ResultActions createTicket(Map<String, Object> body) throws Exception {
        return mvc.perform(post("/api/tickets").header("Authorization", bearer(employee))
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }

    private static Map<String, Object> validTicket() {
        return new HashMap<>(Map.of("title", "Printer offline", "description", "Printer is unreachable",
                "category", "HARDWARE", "priority", "MEDIUM"));
    }

    private static Map<String, Object> changedTicket(String field, Object value) {
        Map<String, Object> body = validTicket();
        if (value == null) {
            body.remove(field);
        } else {
            body.put(field, value);
        }
        return body;
    }

    private static long ticketId(String response) {
        return JsonPath.<Number>read(response, "$.id").longValue();
    }
}
