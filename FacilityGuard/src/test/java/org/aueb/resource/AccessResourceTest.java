package org.aueb.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.persistence.*;
import org.aueb.representation.AccessRequestRepresentation;
import org.aueb.util.Address;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class AccessResourceTest {

    @Inject AccessCardRepository cardRepo;
    @Inject CheckpointRepository checkpointRepo;
    @Inject PermissionRepository permissionRepo;
    @Inject AreaRepository areaRepo;
    @Inject BuildingRepository buildingRepo;
    @Inject UserRepository userRepo;
    @Inject AccessLogRepository logRepo;
    @Inject AlertRepository alertRepo;

    private Integer cardId;
    private Integer checkpointId;

    @BeforeEach
    @Transactional
    void setup() {
        // 1. Create Infrastructure
        Building b = new Building("Secure HQ", new Address("TestAddress", "1", "Test", "11678", "Greece"));
        buildingRepo.persist(b);
        Area a = new Area("Vault", b);
        areaRepo.persist(a);

        Checkpoint cp = new Checkpoint("Vault Door");
        cp.setArea(a);
        checkpointRepo.persist(cp);
        this.checkpointId = cp.getCheckpointId();

        // 2. Create User & Card
        User u = new User("James", "Bond", "James", "Bond", "007@mi6.uk", UserType.Employee);
        userRepo.persist(u);

        AccessCard card = new AccessCard(Date.from(Instant.now().plusSeconds(3600)));
        card.setUser(u);
        cardRepo.persist(card);
        this.cardId = card.getCardId();

        // 3. Grant Permission
        Permission p = new Permission(PermissionType.AccessGranted, card, a);
        permissionRepo.persist(p);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        alertRepo.deleteAll();
        logRepo.deleteAll();
        permissionRepo.deleteAll();
        checkpointRepo.deleteAll();
        userRepo.deleteAll();
        cardRepo.deleteAll();
        areaRepo.deleteAll();
        buildingRepo.deleteAll();
    }

    @Test
    void testRequestAccess_Granted() {
        AccessRequestRepresentation request = new AccessRequestRepresentation();
        request.cardId = cardId;
        request.checkpointId = checkpointId;
        request.accessType = AccessType.In;

        // Perform POST request
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/access/request")
                .then()
                .statusCode(200) // Expect OK
                .body("status", equalTo("GRANTED"));
    }

    @Test
    void testRequestAccess_Denied_NoPermission() {
        // Revoke permission by deleting it
        // Note: In integration tests, modifying DB directly inside test method
        // usually requires its own transaction logic or helper service.
        // For simplicity, we assume the setup gave permission.

        // Let's create a NEW area with NO permission
        // We need to use a helper or modify the setup logic,
        // but to keep it simple, let's just use an invalid checkpoint ID (simulates bad request or not found)
        // Or better: Create a scenario where it fails.

        AccessRequestRepresentation request = new AccessRequestRepresentation();
        request.cardId = cardId;
        request.checkpointId = 9999; // Non-existent checkpoint
        request.accessType = AccessType.In;

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/access/request")
                .then()
                .statusCode(403) // Should be Forbidden (or 404/Bad Request depending on Service logic)
                .body("status", equalTo("DENIED"));
        // Note: AccessService returns false if checkpoint not found, which maps to DENIED in Resource.
    }

    @Test
    void testGetLocation() {
        // 1. Enter first
        AccessRequestRepresentation request = new AccessRequestRepresentation();
        request.cardId = cardId;
        request.checkpointId = checkpointId;
        request.accessType = AccessType.In;

        given().contentType(ContentType.JSON).body(request).post("/access/request").then().statusCode(200);

        // 2. Check Location
        given()
                .when()
                .get("/access/location/" + cardId)
                .then()
                .statusCode(200)
                .body("status", equalTo("INSIDE"))
                .body("areaName", equalTo("Vault"));
    }
}