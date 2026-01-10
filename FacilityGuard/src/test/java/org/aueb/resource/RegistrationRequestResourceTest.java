package org.aueb.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.aueb.domain.RegistrationRequest;
import org.aueb.domain.User;
import org.aueb.persistence.RegistrationRequestRepository;
import org.aueb.persistence.UserRepository;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class RegistrationRequestResourceTest {

    @Inject
    RegistrationRequestRepository requestRepository;

    @Inject
    UserRepository userRepository;

    private int visitorId;
    private int adminId;

    @BeforeEach
    @Transactional
    public void setup() {
        // 1. Καθαρισμός Βάσης (Παιδιά -> Γονείς)
        // Σβήνουμε πρώτα τα αιτήματα
        requestRepository.deleteAll();
        // Σβήνουμε τους χρήστες
        userRepository.deleteAll();

        // 2. Δημιουργία Απλού Χρήστη (Visitor) που θα κάνει αίτηση
        User visitor = new User("visitor", "pass123", "Vis", "Itor", "vis@test.com", UserType.Visitor);
        userRepository.persist(visitor);
        visitorId = visitor.getUserId();

        // 3. Δημιουργία Διαχειριστή (Admin) για εγκρίσεις
        User admin = new User("admin", "adminpass", "Adm", "In", "admin@test.com", UserType.Administrator);
        userRepository.persist(admin);
        adminId = admin.getUserId();
    }

    @Test
    public void testSubmitRequest() {
        // Το DTO περιμένει: public UserRepresentation user; και μέσα public int id;
        String requestBody = "{\"user\": {\"id\": " + visitorId + "}}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/requests")
                .then()
                .statusCode(201) // Created
                .header("Location", containsString("/requests/"))
                .body("user.id", equalTo(visitorId))
                .body("status", equalTo("Active"))
                .body("approved", is(false));
    }

    @Test
    public void testSubmitRequestUserNotFound() {
        String requestBody = "{\"user\": {\"id\": 99999}}"; // Ανύπαρκτο ID

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/requests")
                .then()
                .statusCode(404);
    }

    @Test
    public void testSubmitRequestConflict() {
        // Ο χρήστης έχει ήδη ενεργή αίτηση
        createActiveRequestForUser(visitorId);

        String requestBody = "{\"user\": {\"id\": " + visitorId + "}}";

        // Προσπάθεια δεύτερης υποβολής
        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/requests")
                .then()
                .statusCode(409) // Conflict (IllegalStateException mapped)
                .body(containsString("already has an active registration request"));
    }

    @Test
    public void testGetPendingRequests() {
        createActiveRequestForUser(visitorId);

        given()
                .when()
                .get("/requests/pending")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].user.username", equalTo("visitor"));
    }

    @Test
    public void testApproveRequest() {
        // Δημιουργία αίτησης
        int requestId = createActiveRequestForUser(visitorId);

        // Κλήση approve με το ID του Admin
        given()
                .queryParam("adminId", adminId)
                .when()
                .put("/requests/" + requestId + "/approve")
                .then()
                .statusCode(200)
                .body("approved", is(true))
                .body("status", equalTo("Active"));
    }

    @Test
    public void testRejectRequest() {
        int requestId = createActiveRequestForUser(visitorId);

        given()
                .queryParam("adminId", adminId)
                .when()
                .put("/requests/" + requestId + "/reject")
                .then()
                .statusCode(200)
                .body("approved", is(false))
                .body("status", equalTo("Inactive")); // Όταν απορρίπτεται γίνεται Inactive
    }

    @Test
    public void testApproveRequestForbidden() {
        int requestId = createActiveRequestForUser(visitorId);

        // Προσπάθεια έγκρισης από τον ίδιο τον visitor (που δεν είναι Admin)
        given()
                .queryParam("adminId", visitorId)
                .when()
                .put("/requests/" + requestId + "/approve")
                .then()
                .statusCode(403) // Forbidden (SecurityException)
                .body(containsString("Only users with the role 'ADMINISTRATOR'"));
    }

    // --- Helper Methods ---

    @Transactional
    int createActiveRequestForUser(int userId) {
        User user = userRepository.findById(userId);
        // Χρήση της μεθόδου του Domain
        RegistrationRequest req = user.submitRegistrationRequest();

        // Explicit persist γιατί το Resource κάνει repo.persist(req)
        requestRepository.persist(req);

        return req.getRegistrationId();
    }
}