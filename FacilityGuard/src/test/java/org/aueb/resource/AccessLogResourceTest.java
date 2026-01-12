package org.aueb.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.persistence.*;
import org.aueb.util.Address;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AccessLogResourceTest {

    @Inject BuildingRepository buildingRepo;
    @Inject AreaRepository areaRepo;
    @Inject CheckpointRepository checkpointRepo;
    @Inject UserRepository userRepo;
    @Inject AccessCardRepository cardRepo;
    @Inject PermissionRepository permissionRepo;
    @Inject AccessLogRepository logRepo;
    @Inject AlertRepository alertRepo;
    @Inject RegistrationRequestRepository registrationRequestRepo;
    private Integer logId;
    private Integer cardId;
    private Integer checkpointId;

    @BeforeEach
    @Transactional
    void setup() {

        // 🔹 CLEAN (order matters λόγω FK)
        alertRepo.deleteAll();
        logRepo.deleteAll();
        permissionRepo.deleteAll();
        registrationRequestRepo.deleteAll();
        userRepo.deleteAll();
        cardRepo.deleteAll();
        checkpointRepo.deleteAll();
        areaRepo.deleteAll();
        buildingRepo.deleteAll();



        // 🔹 Infrastructure
        Building b = new Building(
                "HQ",
                new Address("Street", "1", "Athens", "11111", "GR")
        );
        buildingRepo.persist(b);

        Area a = new Area("Vault", b);
        areaRepo.persist(a);

        Checkpoint cp = new Checkpoint("Vault Door");
        cp.setArea(a);
        checkpointRepo.persist(cp);
        checkpointId = cp.getCheckpointId();

        // 🔹 User & Card
        User u = new User(
                "James", "Bond", "james", "bond",
                "jb@mi6.uk", UserType.Employee
        );
        userRepo.persist(u);

        AccessCard card = new AccessCard(
                Date.from(Instant.now().plusSeconds(3600))
        );
        card.setUser(u);
        cardRepo.persist(card);
        cardId = card.getCardId();

        // 🔹 Permission
        Permission p = new Permission(PermissionType.AccessGranted, card, a);
        permissionRepo.persist(p);

        // 🔹 AccessLog
        AccessLog log = new AccessLog(
                PermissionType.AccessGranted,
                AccessType.In,
                card,
                cp
        );
        log.setTimestamp(new Date());
        logRepo.persist(log);
        logId = log.getLogId();
    }

    /* =====================================================
       GET ALL
       ===================================================== */
    @Test
    void testGetAllLogs() {
        given()
                .when().get("/access-logs")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    /* =====================================================
       GET BY ID
       ===================================================== */
    @Test
    void testGetLogById() {
        given()
                .when().get("/access-logs/" + logId)
                .then()
                .statusCode(200)
                .body("logId", equalTo(logId))
                .body("accessGranted", equalTo("AccessGranted"));
    }

    /* =====================================================
       GET BY CARD
       ===================================================== */
    @Test
    void testGetLogsByCard() {
        given()
                .when().get("/access-logs/card/" + cardId)
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }

    /* =====================================================
       GET BY CHECKPOINT
       ===================================================== */
    @Test
    void testGetLogsByCheckpoint() {
        given()
                .when().get("/access-logs/checkpoint/" + checkpointId)
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }

    /* =====================================================
       GET DENIED
       ===================================================== */
    @Test
    void testGetDeniedAccesses() {
        given()
                .when().get("/access-logs/denied")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    /* =====================================================
       GET LAST N
       ===================================================== */
    @Test
    void testGetLastN() {
        given()
                .when().get("/access-logs/last/1")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }

    /* =====================================================
       GET BETWEEN DATES
       ===================================================== */
    @Test
    void testGetBetweenDates() {

        long from = Instant.now().minusSeconds(3600).toEpochMilli();
        long to   = Instant.now().plusSeconds(3600).toEpochMilli();

        given()
                .queryParam("from", from)
                .queryParam("to", to)
                .when().get("/access-logs/between")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }

    /* =====================================================
       DELETE
       ===================================================== */
    @Test
    void testDeleteLog() {

        given()
                .when().delete("/access-logs/" + logId)
                .then()
                .statusCode(204);

        given()
                .when().get("/access-logs/" + logId)
                .then()
                .statusCode(404);
    }
}
