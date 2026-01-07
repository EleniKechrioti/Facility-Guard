package org.aueb.persistence;

import jakarta.persistence.EntityManager;
import org.aueb.domain.*;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AccessCardJPATest extends JPATest{

    @Test
    void testPersistAccessCard() {
        AccessCard card = new AccessCard(new Date());

        em.getTransaction().begin();
        em.persist(card);
        em.getTransaction().commit();
        em.clear();

        AccessCard saved = em.find(AccessCard.class, card.getCardId());
        assertNotNull(saved);
        assertEquals(ActivityStatus.Active, saved.getStatus());
    }

    @Test
    void testCascadePersistPermissions() {

        Building b = Initializer.getPersistedBuilding();
        Building building = em.find(Building.class, b.getBuildingId());
        Area area = building.getAreas().iterator().next();

        AccessCard card = new AccessCard(new Date());

        Permission p1 = new Permission(PermissionType.AccessGranted, card, area);
        Permission p2 = new Permission(PermissionType.AccessDenied, card, area);

        em.getTransaction().begin();
        em.persist(card); /**  cascade persists p1 & p2 (but only 1 kept because of equals/hashCode)  */
        em.getTransaction().commit();
        em.clear();

        AccessCard saved = em.find(AccessCard.class, card.getCardId());
        assertNotNull(saved);

        /** EXPECTED: Only 1 permission because both have id=0 and equals() treats them as duplicates.   */
        assertEquals(1, saved.getPermissions().size(),
                "Only one permission should be stored because equals() compares id=0 for both before persist.");
    }

    @Test
    void testAddAccessLogCascade() {

        AccessCard card = new AccessCard(new Date());
        Checkpoint cp = em.createQuery("SELECT c FROM Checkpoint c", Checkpoint.class)
                .setMaxResults(1)
                .getSingleResult();

        AccessLog log1 = new AccessLog(PermissionType.AccessGranted, org.aueb.util.enumerations.AccessType.In, card, cp);

        em.getTransaction().begin();
        em.persist(card); // cascade persist for AccessLogs
        em.getTransaction().commit();
        em.clear();

        AccessCard saved = em.find(AccessCard.class, card.getCardId());
        assertNotNull(saved);
        assertEquals(1, saved.getAccessLogs().size());
    }

    @Test
    void testDeactivateCard() {

        AccessCard card = new AccessCard(new Date());

        em.getTransaction().begin();
        em.persist(card);
        em.getTransaction().commit();
        em.clear();

        AccessCard saved = em.find(AccessCard.class, card.getCardId());
        assertNotNull(saved);

        saved.deactivateCard();

        em.getTransaction().begin();
        em.merge(saved);
        em.getTransaction().commit();
        em.clear();

        AccessCard updated = em.find(AccessCard.class, card.getCardId());
        assertEquals(ActivityStatus.Inactive, updated.getStatus());
    }

    /**
     * Tests the cascade/orphanRemoval mechanism by removing a Permission from the collection.
     */
    @Test
    void testPermissions_OrphanRemoval() {
        User user = new User("PermUser", "p", "P", "P", "p@p.com", UserType.Employee);
        AccessCard card = new AccessCard(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)));
        user.setAccessCard(card);

        Area areaToPersist = new Area("Perm Zone Alpha", this.testBuilding);

        em.getTransaction().begin();
        em.persist(user);
        em.persist(areaToPersist);
        em.getTransaction().commit();
        em.clear();

        AccessCard managedCard = em.find(AccessCard.class, card.getCardId());
        Area managedArea = em.find(Area.class, areaToPersist.getAreaId());

        Permission perm = new Permission(PermissionType.AccessGranted, managedCard, managedArea);
        managedCard.addPermission(perm);

        em.getTransaction().begin();
        em.persist(perm);
        em.getTransaction().commit();
        em.clear();

        int permissionId = perm.getPermissionId();

        AccessCard cardToModify = em.find(AccessCard.class, managedCard.getCardId());

        em.getTransaction().begin();
        cardToModify.getPermissions().clear();
        em.getTransaction().commit();
        em.clear();

        assertNull(em.find(Permission.class, permissionId), "The Permission must be deleted due to orphanRemoval.");
    }

    /**
     * Tests the cascade persistence to the AccessLog collection (One-to-Many)
     * and verifies the integrity of the inverse User link.
     */
    @Test
    void testAccessLogCollection_andUserLinkIntegrity() {
        User initialUser = new User("LogUser", "p", "L", "L", "l@l.com", UserType.Employee);
        Date date = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        AccessCard card = new AccessCard(date);
        initialUser.setAccessCard(card);

        Area areaToPersist = new Area("Lobby Area", this.testBuilding);
        Checkpoint cp1 = new Checkpoint("Entry Gate");

        cp1.setArea(areaToPersist);

        em.getTransaction().begin();
        em.persist(initialUser);
        em.persist(areaToPersist);
        em.persist(cp1);
        em.getTransaction().commit();
        em.clear();

        User managedUser = em.find(User.class, initialUser.getUserId());
        AccessCard managedCard = managedUser.getAccessCard();
        Checkpoint managedCp1 = em.find(Checkpoint.class, cp1.getCheckpointId());

        AccessLog log1 = new AccessLog(PermissionType.AccessGranted, AccessType.In, card, managedCp1);
        managedCard.addAccessLog(log1);

        em.getTransaction().begin();
        em.persist(log1);
        em.getTransaction().commit();
        em.clear();

        AccessCard retrievedCard = em.find(AccessCard.class, card.getCardId());

        assertEquals(1, retrievedCard.getAccessLogs().size(), "Card must hold exactly 1 AccessLog.");

        assertNotNull(retrievedCard.getUser(), "The inverse link to User must be correctly loaded.");
        assertEquals(managedUser.getUserId(), retrievedCard.getUser().getUserId(),
                "The Card must point back to the correct User ID.");
    }
}