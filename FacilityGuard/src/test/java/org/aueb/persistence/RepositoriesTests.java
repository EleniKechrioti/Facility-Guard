package org.aueb.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@QuarkusTest
public class RepositoriesTests extends JPATest{

    @Inject AlertRepository alertRepo;
    @Inject AccessLogRepository logRepo;
    @Inject CheckpointRepository checkpointRepo;
    @Inject AccessCardRepository cardRepo;
    @Inject AreaRepository areaRepo;
    @Inject BuildingRepository buildingRepo;
    @Inject EntityManager em;

    @AfterEach
    @Transactional
    void tearDown() {
        alertRepo.deleteAll();
        logRepo.deleteAll();
        checkpointRepo.deleteAll();
        cardRepo.deleteAll();
        areaRepo.deleteAll();
        buildingRepo.deleteAll();
    }

    // --- AREA REPOSITORY TESTS ---

    @Test
    @Transactional
    void testAreaFindByBuildingId() {
        Building b1 = new Building("Building Alpha", null);
        buildingRepo.persist(b1);

        Building b2 = new Building("Building Beta", null);
        buildingRepo.persist(b2);
        Area a1 = new Area("Reception", b1);
        Area a2 = new Area("Hallway", b1);
        areaRepo.persist(a1);
        areaRepo.persist(a2);

        Area a3 = new Area("Secret Lab", b2);
        areaRepo.persist(a3);

        // We ask for the areas ONLY of Building Alpha (b1)
        List<Area> result = areaRepo.findByBuildingId(b1.getBuildingId());

        // Assertions
        Assertions.assertEquals(2, result.size(), "Πρέπει να βρει ακριβώς 2 περιοχές για το Building Alpha");

        // We confirm that we have found the right
        boolean foundReception = result.stream().anyMatch(a -> a.getName().equals("Reception"));
        boolean foundLab = result.stream().anyMatch(a -> a.getName().equals("Secret Lab"));

        Assertions.assertTrue(foundReception, "Πρέπει να περιέχει την Reception");
        Assertions.assertFalse(foundLab, "ΔΕΝ πρέπει να περιέχει το Secret Lab του άλλου κτηρίου");
    }
    //----------------------------------

    // --- BUILDING REPOSITORY TESTS ---

    @Test
    @Transactional
    void testBuildingFindByName() {
        Building b = new Building("Central Library", null);
        buildingRepo.persist(b);

        // We are looking by the exact name
        Building found = buildingRepo.findByName("Central Library");
        Assertions.assertNotNull(found, "Πρέπει να βρει το κτίριο");
        Assertions.assertEquals("Central Library", found.getName());

        // We're looking for something that doesn't exist
        Building notFound = buildingRepo.findByName("Gym");
        Assertions.assertNull(notFound, "Δεν πρέπει να βρει κτίριο που δεν υπάρχει");
    }

    @Test
    @Transactional
    void testBuildingSearch() {
        buildingRepo.persist(new Building("Science Lab", null));
        buildingRepo.persist(new Building("Art Studio", null));
        buildingRepo.persist(new Building("Computer Science Lab", null));

        // Δοκιμή: Search by part of the name (case insensitive)
        //We are looking for "lab" -> Must bring "Science Lab" and "Computer Science Lab"
        List<Building> results = buildingRepo.search("lab");

        Assertions.assertEquals(2, results.size(), "Πρέπει να βρει 2 κτίρια που περιέχουν 'lab'");

        // Confirmation that Art Studio did not bring
        boolean containsArt = results.stream().anyMatch(b -> b.getName().contains("Art"));
        Assertions.assertFalse(containsArt);
    }
    //-------------------------------

    // --- ALERT REPOSITORY TESTS ---
    @Test
    @Transactional
    void testListAllSorted() {
        // We make the dependencies (Building -> Area -> Checkpoint -> Card)
        Building b = new Building("B", null); buildingRepo.persist(b);
        Area a = new Area("A", b); areaRepo.persist(a);
        Checkpoint cp = new Checkpoint("CP");
        cp.setArea(a); checkpointRepo.persist(cp);
        AccessCard card = new AccessCard(new Date()); cardRepo.persist(card);

        // Construct a log
        AccessLog log = new AccessLog(PermissionType.AccessDenied, AccessType.In, card, cp);
        log.setTimestamp(new Date());
        logRepo.persist(log);

        // Construct 2 alerts with different timestamps

        // Alert 1 from yesterday
        Alert oldAlert = new Alert(Date.from(Instant.now().minusSeconds(86400)), "Old Alert");
        oldAlert.setAccessLog(log);
        alertRepo.persist(oldAlert);

        // Alert 2 from today
        Alert newAlert = new Alert(new Date(), "New Alert");
        newAlert.setAccessLog(log);
        alertRepo.persist(newAlert);
        List<Alert> alerts = alertRepo.listAllSorted();

        // Check if they came in the correct order (New -> Old)
        Assertions.assertEquals(2, alerts.size());
        Assertions.assertEquals("New Alert", alerts.get(0).getMessage(), "Το πρώτο πρέπει να είναι το πιο πρόσφατο");
        Assertions.assertEquals("Old Alert", alerts.get(1).getMessage());
    }
    //-------------------------------------------------------------------------
}