package org.aueb.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.util.Address;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AccessLogJPATest extends JPATest {

    /**
     * Creates and saves an AccessLog, checking that
     * the relationships with AccessCard and Checkpoint are stored correctly.
     */
    @Test
    @Transactional
    void createAndPersistAccessLog_withCardAndCheckpoint() {

        Address addr = new Address("Test Street", "1", "Test City", "10000", "Greece");

        Building building = new Building("Test Headquarters", addr);
        Area serverRoom = new Area("Server Room 101", building);

        Checkpoint checkpoint = new Checkpoint("Server Room Reader");

        // Relationships
        building.addArea(serverRoom);
        serverRoom.addCheckpoint(checkpoint);

        em.persist(building); // cascades Area & Checkpoint
        em.flush();
        em.clear();

        Area persistentArea = building.getAreas().iterator().next();
        Checkpoint persistentCheckpoint =
                em.find(Checkpoint.class, checkpoint.getCheckpointId());

        assertNotNull(persistentCheckpoint);

        // Create & persist AccessCard
        AccessCard card = new AccessCard(new Date());
        em.persist(card);
        em.flush();
        em.clear();

        AccessCard managedCard = em.find(AccessCard.class, card.getCardId());
        Checkpoint managedCheckpoint =
                em.find(Checkpoint.class, persistentCheckpoint.getCheckpointId());

        // Create & persist AccessLog
        AccessLog log = new AccessLog(
                PermissionType.AccessGranted,
                AccessType.In,
                managedCard,
                managedCheckpoint
        );

        em.persist(log);
        em.flush();
        em.clear();

        // Assertions
        AccessLog retrievedLog = em.find(AccessLog.class, log.getLogId());

        assertNotNull(retrievedLog);
        assertEquals(PermissionType.AccessGranted, retrievedLog.getAccessGranted());
        assertEquals(AccessType.In, retrievedLog.getAccessType());

        assertNotNull(retrievedLog.getAccessCard());
        assertNotNull(retrievedLog.getCheckpoint());

        AccessCard finalCard = em.find(AccessCard.class, managedCard.getCardId());
        assertEquals(1, finalCard.getAccessLogs().size());
    }
}
