package org.aueb.resource;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.persistence.*;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AlertResourceTest {

    @Inject
    AlertRepository alertRepo;
    @Inject
    AccessLogRepository logRepo;
    @Inject
    AccessCardRepository cardRepo;
    @Inject
    CheckpointRepository checkpointRepo;
    @Inject
    AreaRepository areaRepo;
    @Inject
    BuildingRepository buildingRepo;

    /**
     * Καθαρισμός βάσης ΜΕ ΤΗ ΣΩΣΤΗ ΣΕΙΡΑ (Παιδιά -> Γονείς)
     */
    @AfterEach
    @Transactional
    void tearDown() {
        alertRepo.deleteAll();      // 1. Clear Alerts (pointing to Logs)
        logRepo.deleteAll();        // 2. Clear Logs
        checkpointRepo.deleteAll(); // 3. Clear Checkpoints
        cardRepo.deleteAll();       // 4. Clear Cards
        areaRepo.deleteAll();       // 5. Clear Areas
        buildingRepo.deleteAll();   // 6. Clear Buildings
    }

    @BeforeEach
    @Transactional
    void setup() {
        Building building = new Building("Test Building", null); // null address for simplicity
        buildingRepo.persist(building);

        Area area = new Area("Restricted Zone", building);
        areaRepo.persist(area);

        Checkpoint checkpoint = new Checkpoint("Gate 1");
        checkpoint.setArea(area);
        checkpointRepo.persist(checkpoint);

        AccessCard card = new AccessCard(new Date());
        cardRepo.persist(card);

        AccessLog log = new AccessLog(PermissionType.AccessDenied, AccessType.In, card, checkpoint);
        logRepo.persist(log);

        Alert alert = new Alert(new Date(), "Unauthorized Access Attempt");
        alert.setAccessLog(log);
        alertRepo.persist(alert);
    }

    @Test
    void testGetAllAlerts() {
        // We call the API and wait to see the Alert we made in the setup
        given()
                .when()
                .get("/alerts")
                .then()
                .statusCode(200) // It should be OK
                .body("size()", is(1)) // Must have 1 record
                .body("[0].message", equalTo("Unauthorized Access Attempt")) // The message is right
                .body("[0].accessLogId", notNullValue()); // The ID of the log must exist
    }
}