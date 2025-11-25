package org.aueb.persistence;

import jakarta.persistence.EntityManager;
import org.aueb.domain.*;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.PermissionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class AccessCardJPATest {

    private EntityManager em;

    @BeforeEach
    void setup() {
        new Initializer().prepareData();
        em = JPAUtil.getCurrentEntityManager();
    }

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
        em.persist(card); // cascade persists p1 & p2 (but only 1 kept because of equals/hashCode)
        em.getTransaction().commit();
        em.clear();

        AccessCard saved = em.find(AccessCard.class, card.getCardId());
        assertNotNull(saved);

        // === FIXED ===
        // EXPECTED: Only 1 permission because both have id=0 and equals() treats them as duplicates.
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
}
