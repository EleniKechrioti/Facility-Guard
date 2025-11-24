package org.aueb.persistence;

import jakarta.persistence.EntityManager;
import org.aueb.domain.*;
import org.aueb.util.Address;
import org.aueb.util.enumerations.PermissionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class CheckpointJPATest {

    @BeforeEach
    void setup() {
        new Initializer().prepareData();
    }

    @Test
    void testCheckpointPersistence() {

        EntityManager em = JPAUtil.getCurrentEntityManager();
        em.getTransaction().begin();

        // --- Create Building + Area ---
        Address address = new Address("Test St", "10", "Athens", "11111", "Greece");
        Building building = new Building("HQ", address);
        Area area = new Area("Server Room", building);

        em.persist(building);
        em.persist(area);

        // --- Create AccessCard (required by Permission) ---
        AccessCard card = new AccessCard(new Date());
        em.persist(card);

        // --- Create Permission (REQUIRES AccessCard) ---
        Permission permission = new Permission(PermissionType.AccessGranted, card);
        em.persist(permission);

        // --- Create Checkpoint ---
        Checkpoint cp = new Checkpoint("Door Reader A");
        cp.setArea(area);
        cp.setPermission(permission);

        em.persist(cp);

        em.getTransaction().commit();

        // --- Validate Load ---
        EntityManager em2 = JPAUtil.getCurrentEntityManager();
        Checkpoint loaded = em2.find(Checkpoint.class, cp.getCheckpointId());

        assertNotNull(loaded);
        assertEquals("Door Reader A", loaded.getName());
        assertEquals(area.getAreaId(), loaded.getArea().getAreaId());
        assertEquals(permission.getPermissionId(), loaded.getPermission().getPermissionId());
    }
}
