package org.aueb.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.persistence.*;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

@QuarkusTest
public class AccessControlServiceTest {

    @Inject AccessControlService accessService;

    // Repositories
    @Inject AccessCardRepository cardRepo;
    @Inject CheckpointRepository checkpointRepo;
    @Inject PermissionRepository permissionRepo;
    @Inject AreaRepository areaRepo;
    @Inject BuildingRepository buildingRepo;
    @Inject UserRepository userRepo;
    @Inject AccessLogRepository logRepo;
    @Inject AlertRepository alertRepo;

    // Entity Manager for manual cache clearing (essential for integration tests)
    @Inject EntityManager em;

    private Integer cardId;
    private Integer checkpointId;
    private Integer areaId;

    @BeforeEach
    @Transactional
    void setup() {
        // 1. Create Building & Area
        Building building = new Building("Test HQ", null);
        buildingRepo.persist(building);

        Area area = new Area("Server Room", building);
        areaRepo.persist(area);
        this.areaId = area.getAreaId();

        // 2. Create Checkpoint
        Checkpoint cp = new Checkpoint("Main Door");
        cp.setArea(area);
        checkpointRepo.persist(cp);
        this.checkpointId = cp.getCheckpointId();

        // 3. Create User & Active Card
        User user = new User("tester", "password", "Tester", "Testov", "test@aueb.gr", UserType.Visitor);
        userRepo.persist(user);

        // Card expires in the future (approx. 10 hours)
        AccessCard card = new AccessCard(Date.from(Instant.now().plusSeconds(36000)));
        card.setUser(user);
        cardRepo.persist(card);
        this.cardId = card.getCardId();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Cleanup database in correct order to avoid Constraint Violations
        alertRepo.deleteAll();
        logRepo.deleteAll();
        permissionRepo.deleteAll();
        checkpointRepo.deleteAll();

        // Delete User BEFORE Card (due to Foreign Key constraint)
        userRepo.deleteAll();
        cardRepo.deleteAll();

        areaRepo.deleteAll();
        buildingRepo.deleteAll();
    }

    /**
     * SCENARIO 1: Successful Access (Happy Path)
     * User has an active card AND a valid Permission -> Access must be Granted.
     */
    @Test
    @Transactional
    void testAccessGranted() {
        AccessCard card = cardRepo.findById(cardId);
        Area area = areaRepo.findById(areaId);

        // Grant Permission
        Permission perm = new Permission(PermissionType.AccessGranted, card, area);
        permissionRepo.persist(perm);

        // Action
        boolean result = accessService.requestAccess(cardId, checkpointId, AccessType.In);

        // Assert
        Assertions.assertTrue(result, "Access should have been granted");
        Assertions.assertEquals(1, logRepo.count());
        Assertions.assertEquals(PermissionType.AccessGranted, logRepo.listAll().get(0).getAccessGranted());
        Assertions.assertEquals(0, alertRepo.count());
    }

    /**
     * SCENARIO 2: Access Denied due to Lack of Permission (Security)
     * User has a card, but NO Permission exists for this area -> Access Denied + Alert.
     */
    @Test
    @Transactional
    void testAccessDenied_NoPermission() {
        // No permission granted

        // Action
        boolean result = accessService.requestAccess(cardId, checkpointId, AccessType.In);

        // Assert
        Assertions.assertFalse(result, "Access should have been denied");
        Assertions.assertEquals(PermissionType.AccessDenied, logRepo.listAll().get(0).getAccessGranted());
        Assertions.assertEquals(1, alertRepo.count());
        Assertions.assertTrue(alertRepo.listAll().get(0).getMessage().contains("No Permission"));
    }

    /**
     * SCENARIO 3: Access Denied due to Expired Card (Validation)
     */
    @Test
    @Transactional
    void testAccessDenied_ExpiredCard() {
        // Expire the card manually
        AccessCard card = cardRepo.findById(cardId);
        card.setExpirationDate(Date.from(Instant.now().minusSeconds(86400))); // Yesterday

        // Grant Permission (to prove denial is due to card validity, not permission)
        Area area = areaRepo.findById(areaId);
        permissionRepo.persist(new Permission(PermissionType.AccessGranted, card, area));

        // Action
        boolean result = accessService.requestAccess(cardId, checkpointId, AccessType.In);

        // Assert
        Assertions.assertFalse(result, "Access should have been denied (Expired Card)");
        Assertions.assertEquals(1, alertRepo.count());
        Assertions.assertTrue(alertRepo.listAll().get(0).getMessage().contains("Expired"));
    }

    /**
     * SCENARIO 4: Explicit Denial / Blacklist (Authorization)
     * Permission exists but is explicitly set to AccessDenied.
     */
    @Test
    @Transactional
    void testAccessDenied_ExplicitDenial() {
        AccessCard card = cardRepo.findById(cardId);
        Area area = areaRepo.findById(areaId);

        // Explicitly deny access
        Permission perm = new Permission(PermissionType.AccessDenied, card, area);
        permissionRepo.persist(perm);

        // Action
        boolean result = accessService.requestAccess(cardId, checkpointId, AccessType.In);

        // Assert
        Assertions.assertFalse(result);
        Assertions.assertEquals(PermissionType.AccessDenied, logRepo.listAll().get(0).getAccessGranted());
        Assertions.assertEquals(1, alertRepo.count());
    }

    /**
     * SCENARIO 5: Anti-Passback Check (Temporal Consistency)
     * Entry -> Entry (Forbidden at the same location).
     * Entry -> Exit -> Entry (Allowed).
     */
    @Test
    @Transactional
    void testAntiPassback() {
        AccessCard card = cardRepo.findById(cardId);
        Area area = areaRepo.findById(areaId);
        permissionRepo.persist(new Permission(PermissionType.AccessGranted, card, area));

        // 1. First Entry -> OK
        boolean firstEntry = accessService.requestAccess(cardId, checkpointId, AccessType.In);
        Assertions.assertTrue(firstEntry, "First entry should be allowed");

        // --- FLUSH & CLEAR: Sync DB state ---
        em.flush();
        em.clear();

        // 2. Second Entry (without Exit) -> DENIED
        boolean secondEntry = accessService.requestAccess(cardId, checkpointId, AccessType.In);
        Assertions.assertFalse(secondEntry, "Consecutive entry should be denied (Anti-Passback)");

        // 3. Exit -> OK
        em.clear();
        boolean exit = accessService.requestAccess(cardId, checkpointId, AccessType.Out);
        Assertions.assertTrue(exit, "Exit should be allowed");

        // 4. Re-Entry -> OK
        em.clear();
        boolean reEntry = accessService.requestAccess(cardId, checkpointId, AccessType.In);
        Assertions.assertTrue(reEntry, "Re-entry after exit should be allowed");
    }

    /**
     * SCENARIO 6: Topology / Neighbor Check (Spatial Consistency)
     * Attempting to move to a non-neighboring area ("Teleporting").
     * Path: Lobby <-> Corridor <-> Vault.
     * Movement: Lobby -> Vault (Illegal).
     */
    @Test
    @Transactional
    void testTopologyViolation_NotNeighbors() {
        // Setup specialized building
        Building b = new Building("Topology HQ", null);
        buildingRepo.persist(b);

        Area lobby = new Area("Lobby", b);
        Area corridor = new Area("Corridor", b);
        Area vault = new Area("Vault", b);

        // Define Neighbors (Lobby <-> Corridor <-> Vault)
        // Note: Lobby is NOT connected to Vault directly
        lobby.addNeighbor(corridor);
        corridor.addNeighbor(vault);

        areaRepo.persist(lobby);
        areaRepo.persist(corridor);
        areaRepo.persist(vault);

        Checkpoint lobbyDoor = new Checkpoint("Lobby Door"); lobbyDoor.setArea(lobby);
        Checkpoint vaultDoor = new Checkpoint("Vault Door"); vaultDoor.setArea(vault);
        checkpointRepo.persist(lobbyDoor);
        checkpointRepo.persist(vaultDoor);

        // Permissions for all areas
        AccessCard card = cardRepo.findById(cardId);
        permissionRepo.persist(new Permission(PermissionType.AccessGranted, card, lobby));
        permissionRepo.persist(new Permission(PermissionType.AccessGranted, card, corridor));
        permissionRepo.persist(new Permission(PermissionType.AccessGranted, card, vault));

        // 1. Entry to Lobby -> OK
        boolean entryLobby = accessService.requestAccess(cardId, lobbyDoor.getCheckpointId(), AccessType.In);
        Assertions.assertTrue(entryLobby);

        // --- FLUSH & CLEAR ---
        em.flush();
        em.clear();

        // 2. Attempt Entry to Vault (while in Lobby) -> DENIED
        boolean entryVault = accessService.requestAccess(cardId, vaultDoor.getCheckpointId(), AccessType.In);

        // Assert
        Assertions.assertFalse(entryVault, "Entry to Vault should be denied (Not a neighbor)");

        Alert alert = alertRepo.listAllSorted().get(0);
        Assertions.assertTrue(alert.getMessage().contains("Topology Violation"));
    }

    /**
     * SCENARIO 7: Location Tracking / Monitoring
     * Tests the findCurrentLocation method.
     */
    @Test
    @Transactional
    void testFindCurrentLocation() {
        // 1. Initially -> OUTSIDE (null)
        Assertions.assertNull(accessService.findCurrentLocation(cardId));

        // Setup Permission
        AccessCard card = cardRepo.findById(cardId);
        Area area = areaRepo.findById(areaId);
        permissionRepo.persist(new Permission(PermissionType.AccessGranted, card, area));

        // 2. Entry
        accessService.requestAccess(cardId, checkpointId, AccessType.In);
        em.flush();
        em.clear();

        // 3. Check Location -> INSIDE
        Area locationInside = accessService.findCurrentLocation(cardId);
        Assertions.assertNotNull(locationInside);
        Assertions.assertEquals(area.getName(), locationInside.getName());

        // 4. Exit
        accessService.requestAccess(cardId, checkpointId, AccessType.Out);
        em.flush();
        em.clear();

        // 5. Check Location -> OUTSIDE (null)
        Assertions.assertNull(accessService.findCurrentLocation(cardId));
    }
}