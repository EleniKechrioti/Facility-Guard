package org.aueb.persistence;

import org.aueb.domain.*;
import org.aueb.util.Address;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class AccessLogJPATest extends JPATest {

    /**
     * Creates and saves an AccessLog, checking that
     * the relationships with AccessCard and Checkpoint are stored correctly.
     */
    @Test
    void createAndPersistAccessLog_withCardAndCheckpoint() {
        /** We find the Building/Area/Checkpoint that the Initializer has created.   */
        Address addr = new Address("Test Street", "1", "Test City", "10000", "Greece");

        Building persistentBuilding = new Building("Test Headquarters", addr);
        Area serverRoom = new Area("Server Room 101", persistentBuilding);

        /** Checkpoint (Linked with Area)   */
        Checkpoint cp1 = new Checkpoint("Server Room Reader");

        /**  Relationships Connection   */

        /** Building <-> Area   */
        persistentBuilding.addArea(serverRoom);

        /** Area <-> Checkpoint   */
        serverRoom.addCheckpoint(cp1);
        assertNotNull(persistentBuilding, "Test Building must exist from Initializer.");

        Area persistentArea = persistentBuilding.getAreas().iterator().next();
        Checkpoint persistentCheckpoint = persistentArea.getCheckpoints().iterator().next();
        assertNotNull(persistentCheckpoint, "Initializer must have created at least one Checkpoint.");

        /**  We create a new Card   */
        AccessCard card = new AccessCard(new Date());

        em.getTransaction().begin();
        em.persist(card);
        em.getTransaction().commit();
        em.clear();

        /** We re-fetch managed entities */
        AccessCard managedCard = em.find(AccessCard.class, card.getCardId());
        Checkpoint managedCheckpoint = em.find(Checkpoint.class, persistentCheckpoint.getCheckpointId());

        /**  We create and persist the AccessLog   */
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

        /**  Check from the DB  */
        AccessLog retrievedLog = em.find(AccessLog.class, logId);
        assertNotNull(retrievedLog, "AccessLog must be persisted and retrievable.");

        assertEquals(PermissionType.AccessGranted, retrievedLog.getAccessGranted());
        assertEquals(AccessType.In, retrievedLog.getAccessType());

        assertNotNull(retrievedLog.getAccessCard(), "AccessCard relation must be loaded.");
        assertNotNull(retrievedLog.getCheckpoint(), "Checkpoint relation must be loaded.");

        /**  We also check from the card's side (collection)  */
        AccessCard finalCard = em.find(AccessCard.class, managedCard.getCardId());
        assertEquals(1, finalCard.getAccessLogs().size(),
                "AccessCard must contain exactly one AccessLog.");
    }
}