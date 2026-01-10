package org.aueb.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@QuarkusTest
public class RepositoriesTests extends JPATest{

    @Inject AlertRepository alertRepo;
    @Inject AccessLogRepository logRepo;
    @Inject CheckpointRepository checkpointRepo;
    @Inject AccessCardRepository cardRepo;
    @Inject AreaRepository areaRepo;
    @Inject BuildingRepository buildingRepo;
    @Inject UserRepository userRepo;
    @Inject PermissionRepository permissionRepo;
    @Inject RegistrationRequestRepository requestRepo;
    @Inject EntityManager em;

    @BeforeEach
    @Transactional
    void setup() {
        // Η σειρά διαγραφής είναι σημαντική λόγω Foreign Keys!
        alertRepo.deleteAll();
        logRepo.deleteAll();
        permissionRepo.deleteAll(); // Permissions refer to Cards/Areas
        requestRepo.deleteAll();    // Requests refer to Users

        // Checkpoints refer to Areas
        checkpointRepo.deleteAll();

        // Users own Cards (Cascade.ALL), so deleting users deletes cards.
        // However, if we have standalone cards or references, we should be careful.
        // We delete Users first to trigger cascade to Cards.
        userRepo.deleteAll();

        // Just in case any orphan cards remain
        cardRepo.deleteAll();

        areaRepo.deleteAll();       // Areas refer to Buildings
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
    // --- USER REPOSITORY TESTS ---
    @Test
    @Transactional
    void testUserFindByUsernameAndAdmins() {
        // Create Admin
        User admin = new User("admin_test", "pass", "A", "A", "admin@t.com", UserType.Administrator);
        userRepo.persist(admin);

        // Create Employee
        User emp = new User("emp_test", "pass", "E", "E", "emp@t.com", UserType.Employee);
        userRepo.persist(emp);

        // Test findByUsername
        User found = userRepo.findByUsername("admin_test");
        Assertions.assertNotNull(found);
        Assertions.assertEquals(UserType.Administrator, found.getUserType());
        Assertions.assertNull(userRepo.findByUsername("unknown"));

        // Test findAdmins
        List<User> admins = userRepo.findAdmins();
        Assertions.assertEquals(1, admins.size());
        Assertions.assertEquals("admin_test", admins.get(0).getUsername());
    }

    // --- ACCESS CARD REPOSITORY TESTS ---
    @Test
    @Transactional
    void testAccessCardQueries() {
        // Setup User with Card
        User user = new User("card_user", "pass", "C", "C", "card@t.com", UserType.Employee);

        // Card λήγει αύριο (Expiring Soon check)
        Date tomorrow = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        AccessCard card = new AccessCard(tomorrow);
        // Η κάρτα είναι Active by default στον constructor. (Αν όχι, την ενεργοποιούμε):


        user.setAccessCard(card);
        userRepo.persist(user); // Cascades to Card

        // Test findActiveCards
        List<AccessCard> activeCards = cardRepo.findActiveCards();
        Assertions.assertFalse(activeCards.isEmpty());

        // Test findByUserId
        AccessCard foundCard = cardRepo.findByUserId(user.getUserId());
        Assertions.assertNotNull(foundCard);
        Assertions.assertEquals(card.getCardId(), foundCard.getCardId());

        // Test findExpiringSoon (ψάχνουμε κάρτες που λήγουν πριν από "μεθαύριο")
        Date dayAfterTomorrow = Date.from(Instant.now().plus(2, ChronoUnit.DAYS));
        List<AccessCard> expiring = cardRepo.findExpiringSoon(dayAfterTomorrow);
        Assertions.assertTrue(expiring.stream().anyMatch(c -> c.getCardId() == card.getCardId()),
                "Η κάρτα λήγει αύριο, άρα πρέπει να βρεθεί ως expiring soon");
    }

    // --- PERMISSION REPOSITORY TESTS ---
    @Test
    @Transactional
    void testPermissionQueries() {
        // Setup hierarchy
        Building b = new Building("PermBldg", null); buildingRepo.persist(b);
        Area area = new Area("PermArea", b); areaRepo.persist(area);

        User u = new User("perm_u", "p", "P", "P", "p@t.com", UserType.Employee);
        AccessCard card = new AccessCard(new Date());
        u.setAccessCard(card);
        userRepo.persist(u);

        // Create Permission
        Permission p = new Permission(PermissionType.AccessGranted, card, area);
        permissionRepo.persist(p);

        // Test findByCardId
        List<Permission> cardPerms = permissionRepo.findByCardId(card.getCardId());
        Assertions.assertEquals(1, cardPerms.size());
        Assertions.assertEquals(PermissionType.AccessGranted, cardPerms.get(0).getAccessGranted());

        // Test findByCardAndArea
        Optional<Permission> specificPerm = permissionRepo.findByCardAndArea(card.getCardId(), area.getAreaId());
        Assertions.assertTrue(specificPerm.isPresent());

        // Test negative find
        Assertions.assertFalse(permissionRepo.findByCardAndArea(card.getCardId(), 999).isPresent());
    }

    // --- REGISTRATION REQUEST REPOSITORY TESTS ---
    @Test
    @Transactional
    void testRegistrationRequestQueries() {
        User u = new User("req_user", "p", "R", "R", "req@t.com", UserType.Visitor);
        userRepo.persist(u);

        // Create 2 requests: One Pending, One Inactive
        RegistrationRequest req1 = new RegistrationRequest();
        req1.setUser(u);


        RegistrationRequest req2 = new RegistrationRequest();
        req2.setUser(u);
        req2.setApprovedStatus(false, new User("adm", "p","A","A","a@t.com", UserType.Administrator)); // Reject -> Inactive


        requestRepo.persist(req1);
        // Persist req2 manually as inactive (simulating repository logic)

        // Test findPendingRequests
        List<RegistrationRequest> pending = requestRepo.findPendingRequests();
        Assertions.assertTrue(pending.stream().anyMatch(r -> r.getRegistrationId() == req1.getRegistrationId()));

        // Test findByUserId
        List<RegistrationRequest> userRequests = requestRepo.findByUserId(u.getUserId());
        Assertions.assertTrue(userRequests.size() >= 1);

        // Test hasActiveRequest
        Assertions.assertTrue(requestRepo.hasActiveRequest(u.getUserId()));
    }

    // --- CHECKPOINT REPOSITORY TESTS ---
    @Test
    @Transactional
    void testCheckpointFetchByArea() {
        // Setup: Building -> Area -> Checkpoints
        Building b = new Building("CP_Building", null);
        buildingRepo.persist(b);

        Area area1 = new Area("Area 1", b);
        areaRepo.persist(area1);

        Area area2 = new Area("Area 2", b);
        areaRepo.persist(area2);

        Checkpoint cp1 = new Checkpoint("CP-1");
        cp1.setArea(area1);
        checkpointRepo.persist(cp1);

        Checkpoint cp2 = new Checkpoint("CP-2");
        cp2.setArea(area1);
        checkpointRepo.persist(cp2);

        Checkpoint cp3 = new Checkpoint("CP-3");
        cp3.setArea(area2);
        checkpointRepo.persist(cp3);

        // Act: fetch checkpoints only for area1
        List<Checkpoint> result = checkpointRepo.fetchByArea(area1.getAreaId());

        // Assert
        Assertions.assertEquals(2, result.size(), "Πρέπει να επιστρέψει 2 checkpoints για το Area 1");

        boolean foundCP1 = result.stream().anyMatch(c -> c.getName().equals("CP-1"));
        boolean foundCP2 = result.stream().anyMatch(c -> c.getName().equals("CP-2"));
        boolean foundCP3 = result.stream().anyMatch(c -> c.getName().equals("CP-3"));

        Assertions.assertTrue(foundCP1);
        Assertions.assertTrue(foundCP2);
        Assertions.assertFalse(foundCP3, "Δεν πρέπει να περιέχει checkpoints άλλου area");
    }

    // --- ACCESS LOG REPOSITORY TESTS ---
    @Test
    @Transactional
    void testAccessLogPersistAndRetrieve() {
        // Setup hierarchy
        Building b = new Building("LogB", null);
        buildingRepo.persist(b);

        Area area = new Area("LogArea", b);
        areaRepo.persist(area);

        Checkpoint cp = new Checkpoint("LogCP");
        cp.setArea(area);
        checkpointRepo.persist(cp);

        AccessCard card = new AccessCard(new Date());
        cardRepo.persist(card);

        // Create AccessLog
        AccessLog log = new AccessLog(
                PermissionType.AccessGranted,
                AccessType.In,
                card,
                cp
        );
        logRepo.persist(log);

        // Act: retrieve all logs
        List<AccessLog> logs = logRepo.listAll();

        // Assert
        Assertions.assertEquals(1, logs.size(), "Πρέπει να υπάρχει ακριβώς ένα AccessLog");

        AccessLog stored = logs.get(0);
        Assertions.assertEquals(PermissionType.AccessGranted, stored.getAccessGranted());
        Assertions.assertEquals(AccessType.In, stored.getAccessType());
        Assertions.assertNotNull(stored.getAccessCard());
        Assertions.assertNotNull(stored.getCheckpoint());
    }




}