package org.aueb.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.aueb.domain.User;
import org.aueb.persistence.*;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class UserResourceTest {

    @Inject
    UserRepository userRepository;

    // Injecting all other repos for full cleanup to avoid FK violations
    @Inject
    RegistrationRequestRepository requestRepository;
    @Inject
    PermissionRepository permissionRepository;
    @Inject
    AccessCardRepository cardRepository;
    @Inject
    AreaRepository areaRepository;
    @Inject
    BuildingRepository buildingRepository;
    @Inject
    CheckpointRepository checkpointRepository;
    @Inject
    AccessLogRepository accessLogRepository;
    @Inject
    AlertRepository alertRepository;

    private int testUserId;

    @BeforeEach
    @Transactional
    public void setup() {
        // 1. Καθαρισμός Βάσης (Από τα "παιδιά" προς τους "γονείς")

        // Καθαρισμός Logs και Alerts και Checkpoints (που μπορεί να έμειναν από άλλα tests)
        alertRepository.deleteAll();
        accessLogRepository.deleteAll();
        checkpointRepository.deleteAll();

        // Καθαρισμός Permissions, Requests, Users, Cards
        permissionRepository.deleteAll();
        requestRepository.deleteAll();
        userRepository.deleteAll();
        cardRepository.deleteAll();

        // Τέλος, καθαρισμός Areas και Buildings
        areaRepository.deleteAll();
        buildingRepository.deleteAll();

        // 2. Δημιουργία ενός αρχικού χρήστη για τα tests (Get, Update, Delete)
        User user = new User("existingUser", "pass123", "John", "Doe", "john@test.com", UserType.Employee);
        userRepository.persist(user);
        testUserId = user.getUserId();
    }

    @Test
    public void testGetAllUsers() {
        given()
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].username", equalTo("existingUser"));
    }

    @Test
    public void testGetUserById() {
        given()
                .pathParam("id", testUserId)
                .when()
                .get("/users/{id}")
                .then()
                .statusCode(200)
                .body("id", equalTo(testUserId))
                .body("username", equalTo("existingUser"))
                .body("userType", equalTo("Employee"));
    }

    @Test
    public void testGetUserByIdNotFound() {
        given()
                .pathParam("id", 99999)
                .when()
                .get("/users/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    public void testCreateUser() {
        // Χρησιμοποιούμε το UserCreationRequest DTO που απαιτεί password
        String requestBody = "{"
                + "\"username\": \"newUser\","
                + "\"password\": \"secret\","
                + "\"firstName\": \"Maria\","
                + "\"lastName\": \"Papadopoulou\","
                + "\"email\": \"maria@test.com\","
                + "\"userType\": \"Visitor\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/users")
                .then()
                .statusCode(201)
                .header("Location", containsString("/users/"))
                .body("username", equalTo("newUser"))
                .body("firstName", equalTo("Maria"))
                // Το password δεν πρέπει να επιστρέφεται στο response
                .body("password", nullValue());
    }

    @Test
    public void testCreateUserUsernameConflict() {
        // Προσπάθεια δημιουργίας χρήστη με username που υπάρχει ήδη (existingUser)
        String requestBody = "{"
                + "\"username\": \"existingUser\","
                + "\"password\": \"pass\","
                + "\"firstName\": \"Copy\","
                + "\"lastName\": \"Cat\","
                + "\"email\": \"copy@test.com\","
                + "\"userType\": \"Visitor\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/users")
                .then()
                .statusCode(409) // Conflict
                .body(containsString("Username already exists"));
    }

    @Test
    public void testUpdateUser() {
        // Στο Update χρησιμοποιούμε το UserRepresentation (χωρίς password)
        String requestBody = "{"
                + "\"id\": " + testUserId + ","
                + "\"username\": \"existingUser\","
                + "\"firstName\": \"UpdatedName\","
                + "\"lastName\": \"UpdatedLast\","
                + "\"email\": \"updated@test.com\","
                + "\"userType\": \"Administrator\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("id", testUserId)
                .when()
                .put("/users/{id}")
                .then()
                .statusCode(200)
                .body("firstName", equalTo("UpdatedName"))
                .body("email", equalTo("updated@test.com"))
                .body("userType", equalTo("Administrator"));
    }

    @Test
    public void testUpdateUserNotFound() {
        String requestBody = "{\"firstName\": \"Ghost\"}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .pathParam("id", 99999)
                .when()
                .put("/users/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    public void testDeleteUser() {
        given()
                .pathParam("id", testUserId)
                .when()
                .delete("/users/{id}")
                .then()
                .statusCode(204); // No Content

        // Επιβεβαίωση ότι διαγράφηκε
        given()
                .pathParam("id", testUserId)
                .when()
                .get("/users/{id}")
                .then()
                .statusCode(404);
    }

    @Test
    public void testDeleteUserNotFound() {
        given()
                .pathParam("id", 99999)
                .when()
                .delete("/users/{id}")
                .then()
                .statusCode(404);
    }
}