package org.aueb.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.util.Address;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AccessCardJPATest extends JPATest {

    @Test
    @Transactional
    void testPersistAccessCard() {
        AccessCard card = new AccessCard(new Date());

        // Αντικατάσταση του begin/commit με persist/flush
        em.persist(card);
        em.flush(); // Στέλνει την SQL στη βάση
        em.clear(); // Καθαρίζει την cache (detaches objects)

        AccessCard saved = em.find(AccessCard.class, card.getCardId());
        assertNotNull(saved);
        assertEquals(ActivityStatus.Active, saved.getStatus());
    }

    @Test
    @Transactional
    void testCascadePersistPermissions() {
        // Δημιουργία και persist των Dependencies πρώτα
        Address addr = new Address("Test Street", "1", "Test City", "10000", "Greece");
        Building b = new Building("Test Headquarters", addr);
        Area serverRoom = new Area("Server Room 101", b);
        Checkpoint cp1 = new Checkpoint("Server Room Reader");

        // Σύνδεση σχέσεων
        b.addArea(serverRoom);
        serverRoom.addCheckpoint(cp1);

        // ΠΡΟΣΟΧΗ: Πρέπει να τα σώσουμε για να πάρουν IDs
        em.persist(b);
        em.persist(serverRoom);
        em.persist(cp1);
        em.flush();

        AccessCard card = new AccessCard(new Date());

        // Φτιάχνουμε τα permissions
        Permission p1 = new Permission(PermissionType.AccessGranted, card, serverRoom);
        Permission p2 = new Permission(PermissionType.AccessDenied, card, serverRoom); // Duplicate logic test

        // Προσθήκη στην κάρτα (σημαντικό για το cascade)
        card.addPermission(p1);
        card.addPermission(p2);

        em.persist(card); // cascade persists permissions
        em.flush();
        em.clear();

        AccessCard saved = em.find(AccessCard.class, card.getCardId());
        assertNotNull(saved);

        // Έλεγχος: Λόγω equals/hashCode (αν βασίζονται σε business key ή ID),
        // το Set θα κρατήσει μόνο το ένα αν θεωρούνται ίδια.
        // Αν η equals ελέγχει μόνο ID (που είναι 0 πριν το save), τότε θα κρατήσει 1.
        assertEquals(1, saved.getPermissions().size(),
                "Only one permission should be stored because equals() treats them as duplicates before persist.");
    }

    @Test
    @Transactional
    void testAddAccessLogCascade() {
        // Setup Dependencies
        Address addr = new Address("Log St", "2", "Log City", "20000", "Greece");
        Building b = new Building("Log HQ", addr);
        Area area = new Area("Log Area", b);
        Checkpoint cp = new Checkpoint("Log CP");
        b.addArea(area);
        area.addCheckpoint(cp);

        em.persist(b);
        em.persist(area);
        em.persist(cp);
        em.flush();

        AccessCard card = new AccessCard(new Date());
        AccessLog log1 = new AccessLog(PermissionType.AccessGranted, AccessType.In, card, cp);

        // Σύνδεση για Cascade
        card.addAccessLog(log1);

        em.persist(card); // cascade persist for AccessLogs
        em.flush();
        em.clear();

        AccessCard saved = em.find(AccessCard.class, card.getCardId());
        assertNotNull(saved);
        assertEquals(1, saved.getAccessLogs().size());
    }

    @Test
    @Transactional
    void testDeactivateCard() {
        AccessCard card = new AccessCard(new Date());
        em.persist(card);
        em.flush();
        em.clear();

        AccessCard saved = em.find(AccessCard.class, card.getCardId());
        assertNotNull(saved);

        // Domain Logic Call
        saved.deactivateCard();

        em.merge(saved); // Update
        em.flush();
        em.clear();

        AccessCard updated = em.find(AccessCard.class, card.getCardId());
        assertEquals(ActivityStatus.Inactive, updated.getStatus());
    }

    @Test
    @Transactional
    void testPermissions_OrphanRemoval() {
        // Setup User & Card
        User user = new User("PermUser", "p", "P", "P", "p@p.com", UserType.Employee);
        AccessCard card = new AccessCard(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)));
        user.setAccessCard(card); // Cascade User -> Card

        // Setup Building/Area
        Address addr = new Address("Orphan St", "3", "Orphan City", "30000", "Greece");
        Building b = new Building("Orphan HQ", addr);
        Area area = new Area("Orphan Zone", b);
        b.addArea(area);

        em.persist(user); // Persists User & Card
        em.persist(b);    // Persists Building & Area
        em.flush();

        // Retrieve managed entities
        AccessCard managedCard = em.find(AccessCard.class, card.getCardId());
        Area managedArea = em.find(Area.class, area.getAreaId());

        // Add Permission
        Permission perm = new Permission(PermissionType.AccessGranted, managedCard, managedArea);
        managedCard.addPermission(perm); // Add to collection

        em.flush(); // Save permission via cascade merge/persist
        em.clear();

        int permissionId = perm.getPermissionId();
        assertTrue(permissionId > 0); // Verify it was saved

        // --- ORPHAN REMOVAL TEST ---
        AccessCard cardToModify = em.find(AccessCard.class, managedCard.getCardId());

        // Remove from collection
        cardToModify.getPermissions().clear();

        em.flush(); // Hibernate should execute DELETE
        em.clear();

        assertNull(em.find(Permission.class, permissionId), "The Permission must be deleted due to orphanRemoval.");
    }

    @Test
    @Transactional
    void testAccessLogCollection_andUserLinkIntegrity() {
        User initialUser = new User("LogUser2", "p", "L", "L", "l2@l.com", UserType.Employee);
        Date date = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        AccessCard card = new AccessCard(date);
        initialUser.setAccessCard(card);

        Address addr = new Address("Link St", "4", "Link City", "40000", "Greece");
        Building b = new Building("Link HQ", addr);
        Area area = new Area("Link Area", b);
        Checkpoint cp1 = new Checkpoint("Link CP");
        b.addArea(area);
        area.addCheckpoint(cp1);
        cp1.setArea(area);

        em.persist(initialUser);
        em.persist(b);
        em.persist(area);
        em.persist(cp1);
        em.flush();
        em.clear();

        // Retrieve to ensure clean state
        User managedUser = em.find(User.class, initialUser.getUserId());
        AccessCard managedCard = managedUser.getAccessCard();
        Checkpoint managedCp1 = em.find(Checkpoint.class, cp1.getCheckpointId());

        // Add Access Log
        AccessLog log1 = new AccessLog(PermissionType.AccessGranted, AccessType.In, managedCard, managedCp1);
        managedCard.addAccessLog(log1); // Update collection

        em.persist(log1); // ή em.merge(managedCard) αν έχει cascade
        em.flush();
        em.clear();

        AccessCard retrievedCard = em.find(AccessCard.class, card.getCardId());
        assertEquals(1, retrievedCard.getAccessLogs().size(), "Card must hold exactly 1 AccessLog.");

        assertNotNull(retrievedCard.getUser(), "The inverse link to User must be correctly loaded.");
        assertEquals(managedUser.getUserId(), retrievedCard.getUser().getUserId(),
                "The Card must point back to the correct User ID.");
    }
}