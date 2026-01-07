package org.aueb.persistence;

import org.aueb.domain.*;
import org.aueb.util.Address;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.Test;
import jakarta.persistence.Query;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class PermissionJPATest extends JPATest {

    private <T> T getFirstResult(Query query) {
        List<T> results = query.getResultList();
        assertTrue(results.size() >= 1, "The query must return at least one result.");
        return results.get(0);
    }

    private AccessCard createPersistentParents() {
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
        Area area = new Area("Test Zone Alpha", persistentBuilding);
        User user = new User("PermsUser", "p", "P", "P", "p@p.com", UserType.Employee);
        AccessCard card = new AccessCard(new Date());

        user.setAccessCard(card);

        em.getTransaction().begin();
        em.persist(user);
        em.persist(area);
        em.getTransaction().commit();
        em.clear();

        return em.find(AccessCard.class, card.getCardId());
    }

    @Test
    void testPersistPermission_LinksToCardAndArea() {
        AccessCard managedCard = createPersistentParents();

        Query areaQuery = em.createQuery("select a from Area a where a.name = 'Test Zone Alpha'", Area.class);
        Area managedArea = getFirstResult(areaQuery);

        Permission initialPermission = new Permission(PermissionType.AccessDenied, managedCard, managedArea);

        em.getTransaction().begin();
        em.persist(initialPermission);
        em.getTransaction().commit();
        em.clear();

        Permission retrievedPermission = em.find(Permission.class, initialPermission.getPermissionId());

        assertNotNull(retrievedPermission, "Permission must be retrieved successfully.");
        assertEquals(managedCard.getCardId(), retrievedPermission.getAccessCard().getCardId(), "Must be linked to the correct AccessCard.");
        assertEquals(managedArea.getAreaId(), retrievedPermission.getArea().getAreaId(), "Must be linked to the correct Area.");
    }

    /**
     * Tests updating the PermissionType of a Permission entity,
     * including security checks based on User roles.
     */
    @Test
    void testUpdatePermissionType_AndSecurityCheck() {
        AccessCard managedCard = createPersistentParents();

        Query areaQuery = em.createQuery("select a from Area a where a.name = 'Test Zone Alpha'", Area.class);
        Area managedArea = getFirstResult(areaQuery);

        Query adminQuery = em.createQuery("select u from User u where u.userType = :type", User.class);
        adminQuery.setParameter("type", UserType.Administrator);
        User adminUser = getFirstResult(adminQuery);

        Query employeeQuery = em.createQuery("select u from User u where u.userType = :type", User.class);
        employeeQuery.setParameter("type", UserType.Employee);
        User employeeUser = getFirstResult(employeeQuery);


        Permission permission = new Permission(PermissionType.AccessDenied, managedCard, managedArea);

        em.getTransaction().begin();
        em.persist(permission);
        em.getTransaction().commit();
        em.clear();

        Permission managedPermission = em.find(Permission.class, permission.getPermissionId());

        assertThrows(SecurityException.class, () -> {
            managedPermission.updatePermissionType(PermissionType.AccessGranted, employeeUser);
        }, "Update must fail for a non-Administrator user.");

        em.getTransaction().begin();
        managedPermission.updatePermissionType(PermissionType.AccessGranted, adminUser);
        em.getTransaction().commit();
        em.clear();

        Permission finalPermission = em.find(Permission.class, permission.getPermissionId());

        assertEquals(PermissionType.AccessGranted, finalPermission.getAccessGranted(), "The PermissionType must be updated and persisted.");
    }

    /**
     * Tests the successful deletion of a Permission entity.
     */
    @Test
    void testDeletePermission_Success() {
        AccessCard managedCard = createPersistentParents();
        Area managedArea = em.createQuery("select a from Area a where a.name = 'Test Zone Alpha'", Area.class).getSingleResult();

        Permission permissionToDelete = new Permission(PermissionType.AccessDenied, managedCard, managedArea);

        em.getTransaction().begin();
        em.persist(permissionToDelete);
        em.getTransaction().commit();

        int permissionId = permissionToDelete.getPermissionId();
        em.clear();

        em.getTransaction().begin();
        Permission managedPermission = em.find(Permission.class, permissionId);
        assertNotNull(managedPermission, "Permission must be managed before deletion.");

        em.remove(managedPermission);
        em.getTransaction().commit();
        em.clear();

        assertNull(em.find(Permission.class, permissionId),
                "The Permission entity must be successfully deleted from the database.");
    }
}