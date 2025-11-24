package org.aueb.persistence;

import org.aueb.domain.*;
import org.aueb.util.Address;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JPA tests για την οντότητα AccessLog.
 */
public class AccessLogJPATest extends JPATest {

    /**
     * Δημιουργεί και αποθηκεύει ένα AccessLog, ελέγχοντας ότι
     * οι σχέσεις με AccessCard και Checkpoint αποθηκεύονται σωστά.
     */
    @Test
    void createAndPersistAccessLog_withCardAndCheckpoint() {
        // 1. Βρίσκουμε το Building/Area/Checkpoint που έχει φτιάξει ο Initializer
        Building persistentBuilding = em.find(Building.class, testBuilding.getBuildingId());
        assertNotNull(persistentBuilding, "Test Building must exist from Initializer.");

        Area persistentArea = persistentBuilding.getAreas().iterator().next();
        Checkpoint persistentCheckpoint = persistentArea.getCheckpoints().iterator().next();
        assertNotNull(persistentCheckpoint, "Initializer must have created at least one Checkpoint.");

        // 2. Δημιουργούμε μια νέα κάρτα
        AccessCard card = new AccessCard(new Date());

        em.getTransaction().begin();
        em.persist(card);
        em.getTransaction().commit();
        em.clear();

        // 3. Ξαναπαίρνουμε managed entities
        AccessCard managedCard = em.find(AccessCard.class, card.getCardId());
        Checkpoint managedCheckpoint = em.find(Checkpoint.class, persistentCheckpoint.getCheckpointId());

        // 4. Δημιουργούμε και αποθηκεύουμε το AccessLog
        AccessLog log = new AccessLog(
                PermissionType.AccessGranted,
                AccessType.In,
                managedCard,
                managedCheckpoint
        );

        em.getTransaction().begin();
        em.persist(log);
        em.getTransaction().commit();
        em.clear();

        int logId = log.getLogId();

        // 5. Έλεγχος από τη ΒΔ
        AccessLog retrievedLog = em.find(AccessLog.class, logId);
        assertNotNull(retrievedLog, "AccessLog must be persisted and retrievable.");

        assertEquals(PermissionType.AccessGranted, retrievedLog.getAccessGranted());
        assertEquals(AccessType.In, retrievedLog.getAccessType());

        assertNotNull(retrievedLog.getAccessCard(), "AccessCard relation must be loaded.");
        assertNotNull(retrievedLog.getCheckpoint(), "Checkpoint relation must be loaded.");

        // 6. Ελέγχουμε και από την πλευρά της κάρτας (collection)
        AccessCard finalCard = em.find(AccessCard.class, managedCard.getCardId());
        assertEquals(1, finalCard.getAccessLogs().size(),
                "AccessCard must contain exactly one AccessLog.");
    }
}
