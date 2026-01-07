package org.aueb.persistence;

import jakarta.persistence.EntityManager;
import org.aueb.domain.*;
import org.aueb.util.Address;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class AlertJPATest {

    private EntityManager em;

    @Test
    void testPersistAlertAndAccessLog() {

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
        Building building = em.find(Building.class, persistentBuilding.getBuildingId());
        Area area = building.getAreas().iterator().next();
        Checkpoint cp = area.getCheckpoints().iterator().next();

        /** Create Access Card  */
        AccessCard card = new AccessCard(new Date());

        em.getTransaction().begin();
        em.persist(card);
        em.getTransaction().commit();
        em.clear();

        /** Create AccessLog  */
        AccessCard managedCard = em.find(AccessCard.class, card.getCardId());
        Checkpoint managedCp = em.find(Checkpoint.class, cp.getCheckpointId());

        AccessLog log = new AccessLog(
                PermissionType.AccessDenied,
                AccessType.In,
                managedCard,
                managedCp
        );

        em.getTransaction().begin();
        em.persist(log);
        em.getTransaction().commit();
        em.clear();

        /**  Create Alert linked to the log  */
        Alert alert = new Alert(new Date(), "Unauthorized access attempt");
        em.getTransaction().begin();
        alert.setAccessLog(em.find(AccessLog.class, log.getLogId()));
        em.persist(alert);
        em.getTransaction().commit();
        em.clear();

        /** Retrieve & assert */
        Alert savedAlert = em.find(Alert.class, alert.getAlertId());
        assertNotNull(savedAlert);

        assertEquals("Unauthorized access attempt", savedAlert.getMessage());
        assertNotNull(savedAlert.getAccessLog());

        /** Ensure linking is correct   */
        assertEquals(
                log.getLogId(),
                savedAlert.getAccessLog().getLogId()
        );
    }
}