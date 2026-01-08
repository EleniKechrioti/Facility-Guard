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
public class AlertJPATest extends JPATest {

    @Test
    @Transactional
    void testPersistAlertAndAccessLog() {

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

        // Create AccessCard
        AccessCard card = new AccessCard(new Date());
        em.persist(card);
        em.flush();
        em.clear();

        AccessCard managedCard = em.find(AccessCard.class, card.getCardId());
        Checkpoint managedCheckpoint =
                em.find(Checkpoint.class, checkpoint.getCheckpointId());

        // Create AccessLog
        AccessLog log = new AccessLog(
                PermissionType.AccessDenied,
                AccessType.In,
                managedCard,
                managedCheckpoint
        );

        em.persist(log);
        em.flush();
        em.clear();

        // Create Alert linked to AccessLog
        Alert alert = new Alert(new Date(), "Unauthorized access attempt");
        alert.setAccessLog(em.find(AccessLog.class, log.getLogId()));
        em.persist(alert);

        em.flush();
        em.clear();

        // Assertions
        Alert savedAlert = em.find(Alert.class, alert.getAlertId());
        assertNotNull(savedAlert);
        assertEquals("Unauthorized access attempt", savedAlert.getMessage());
        assertNotNull(savedAlert.getAccessLog());
        assertEquals(log.getLogId(),
                savedAlert.getAccessLog().getLogId());
    }
}
