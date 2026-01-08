package org.aueb.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.util.Address;
import org.aueb.util.enumerations.PermissionType;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CheckpointJPATest extends JPATest {

    @Test
    @Transactional
    void testCheckpointPersistence() {

        // Create Building + Area
        Address address = new Address("Test St", "10", "Athens", "11111", "Greece");
        Building building = new Building("HQ", address);
        Area area = new Area("Server Room", building);

        building.addArea(area);
        em.persist(building); // cascades Area
        em.flush();

        // Create AccessCard
        AccessCard card = new AccessCard(new Date());
        em.persist(card);

        // Create Permission
        Permission permission =
                new Permission(PermissionType.AccessGranted, card, area);
        em.persist(permission);

        // Create Checkpoint
        Checkpoint cp = new Checkpoint("Door Reader A");
        cp.setArea(area);
        cp.setPermission(permission);
        em.persist(cp);

        em.flush();
        em.clear();

        // Validate Load
        Checkpoint loaded =
                em.find(Checkpoint.class, cp.getCheckpointId());

        assertNotNull(loaded);
        assertEquals("Door Reader A", loaded.getName());
        assertEquals(area.getAreaId(), loaded.getArea().getAreaId());
        assertEquals(permission.getPermissionId(),
                loaded.getPermission().getPermissionId());
    }
}
