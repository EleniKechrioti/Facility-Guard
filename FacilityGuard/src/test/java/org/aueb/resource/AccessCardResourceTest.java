package org.aueb.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.persistence.*;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AccessCardResourceTest {

    @Inject
    UserRepository userRepository;

    @Inject
    AccessCardRepository cardRepository;

    @Inject
    AreaRepository areaRepository;

    @Inject
    PermissionRepository permissionRepository;

    @Inject
    BuildingRepository buildingRepository;

    // Helper μεταβλητές για τα tests
    private int testUserId;
    private int testAreaId;

    @BeforeEach
    @Transactional
    public void setup() {
        // --- ΚΑΘΑΡΙΣΜΟΣ ΒΑΣΗΣ ΜΕ ΤΗ ΣΩΣΤΗ ΣΕΙΡΑ ---
        permissionRepository.deleteAll();
        // Διαγραφή RegistrationRequests μέσω JPQL
        userRepository.getEntityManager().createQuery("DELETE FROM RegistrationRequest").executeUpdate();
        userRepository.deleteAll();
        cardRepository.deleteAll();
        areaRepository.deleteAll();
        buildingRepository.deleteAll();

        // --- ΔΗΜΙΟΥΡΓΙΑ ΔΕΔΟΜΕΝΩΝ ---

        // 1. Δημιουργία Χρήστη
        User user = new User("testuser", "password", "Test", "User", "test@aueb.gr", UserType.Visitor);
        userRepository.persist(user);
        testUserId = user.getUserId();

        // 2. Δημιουργία Κτιρίου
        Building building = new Building();
        building.setName("Main Building");
        buildingRepository.persist(building);

        // 3. Δημιουργία Περιοχής
        Area area = new Area();
        area.setName("Server Room");
        area.setBuilding(building);
        areaRepository.persist(area);
        testAreaId = area.getAreaId();
    }

    @Test
    public void testIssueCard() {
        // Ο χρήστης πρέπει να έχει ΕΓΚΕΚΡΙΜΕΝΟ αίτημα
        makeUserEligibleForCard(testUserId);

        String requestBody = "{"
                + "\"userId\": " + testUserId + ","
                + "\"expirationDate\": \"2030-01-01T10:00:00.000Z\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/cards/issue")
                .then()
                .statusCode(201)
                .header("Location", containsString("/cards/"))
                // ΔΙΟΡΘΩΣΗ: Έλεγχος status αντί για isActive
                .body("status", is("Active"));
    }

    @Test
    public void testIssueCardUserNotFound() {
        String requestBody = "{"
                + "\"userId\": 99999,"
                + "\"expirationDate\": \"2030-01-01T10:00:00.000Z\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/cards/issue")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetCardById() {
        int cardId = createCardForUser(testUserId);

        given()
                .pathParam("id", cardId)
                .when()
                .get("/cards/{id}")
                .then()
                .statusCode(200)
                .body("id", equalTo(cardId))
                // Έλεγχος status
                .body("status", is("Active"));
    }

    @Test
    public void testGetCardByUserId() {
        int cardId = createCardForUser(testUserId);

        given()
                .pathParam("userId", testUserId)
                .when()
                .get("/cards/user/{userId}")
                .then()
                .statusCode(200)
                .body("id", equalTo(cardId));
    }

    @Test
    public void testDeactivateCard() {
        int cardId = createCardForUser(testUserId);

        given()
                .pathParam("id", cardId)
                .when()
                .put("/cards/{id}/deactivate")
                .then()
                .statusCode(200)
                // ΔΙΟΡΘΩΣΗ: Έλεγχος status για Inactive
                .body("status", is("Inactive"));
    }

    @Test
    public void testGrantPermission() {
        int cardId = createCardForUser(testUserId);

        String requestBody = "{"
                + "\"areaId\": " + testAreaId + ","
                + "\"type\": \"AccessGranted\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .pathParam("id", cardId)
                .body(requestBody)
                .when()
                .post("/cards/{id}/permissions")
                .then()
                .statusCode(201)
                // Έλεγχος Enum ως String
                .body("accessGranted", equalTo("AccessGranted"))
                .body("area.id", equalTo(testAreaId));
    }

    @Test
    public void testGetCardPermissions() {
        int cardId = createCardForUser(testUserId);

        addPermissionToCard(cardId, testAreaId);

        given()
                .pathParam("id", cardId)
                .when()
                .get("/cards/{id}/permissions")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].area.name", equalTo("Server Room"));
    }

    // --- Helpers ---

    @Transactional
    void makeUserEligibleForCard(int userId) {
        User user = userRepository.findById(userId);

        RegistrationRequest req = new RegistrationRequest();
        req.setRequestDate(new Date());

        User admin = new User("admin", "pass", "Admin", "User", "admin@a.gr", UserType.Administrator);

        req.setApprovedStatus(true, admin);
        req.setStatus(ActivityStatus.Active);

        req.setUser(user);

        userRepository.getEntityManager().merge(user);
    }

    @Transactional
    int createCardForUser(int userId) {
        User user = userRepository.findById(userId);
        AccessCard card = new AccessCard(new Date());
        user.setAccessCard(card);
        cardRepository.persist(card);
        return card.getCardId();
    }

    @Transactional
    void addPermissionToCard(int cardId, int areaId) {
        AccessCard card = cardRepository.findById(cardId);
        Area area = areaRepository.findById(areaId);
        Permission p = new Permission(PermissionType.AccessGranted, card, area);
        permissionRepository.persist(p);
    }
}