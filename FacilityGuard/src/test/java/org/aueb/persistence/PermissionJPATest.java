package org.aueb.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.util.Address;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PermissionJPATest {

    @Inject
    EntityManager em;

    /**
     * Βοηθητική μέθοδος για ανάκτηση αποτελέσματος
     */
    private <T> T getFirstResult(TypedQuery<T> query) {
        List<T> results = query.getResultList();
        assertFalse(results.isEmpty(), "The query must return at least one result.");
        return results.get(0);
    }

    /**
     * Δημιουργία μοναδικού αναγνωριστικού για κάθε test run
     * ώστε να μην έχουμε Unique Constraint Violations (π.χ. στο Username).
     */
    private String generateUniqueId() {
        return "_" + System.nanoTime() + "_" + new Random().nextInt(1000);
    }

    /**
     * Δημιουργία και αποθήκευση των γονικών οντοτήτων.
     */
    private AccessCard createPersistentParents(String uniqueSuffix) {
        // === Create Entities ===
        Address addr = new Address("Test Street", "1", "Test City", "10000", "Greece");
        Building persistentBuilding = new Building("Test Headquarters" + uniqueSuffix, addr);
        Area serverRoom = new Area("Server Room" + uniqueSuffix, persistentBuilding);
        Checkpoint cp1 = new Checkpoint("Reader" + uniqueSuffix);

        // Relationships
        persistentBuilding.addArea(serverRoom);
        serverRoom.addCheckpoint(cp1);

        // Area για το Permission (χρειαζόμαστε μοναδικό όνομα για να το βρούμε μετά με query)
        String areaName = "Test Zone Alpha" + uniqueSuffix;
        Area area = new Area(areaName, persistentBuilding);

        // Users (Admin & Employee) - ΠΡΟΣΟΧΗ: Μοναδικά Usernames/Emails
        User employee = new User("User" + uniqueSuffix, "p", "P", "P", "user" + uniqueSuffix + "@test.com", UserType.Employee);
        User admin = new User("Admin" + uniqueSuffix, "a", "A", "A", "admin" + uniqueSuffix + "@test.com", UserType.Administrator);

        AccessCard card = new AccessCard(new Date());
        employee.setAccessCard(card);

        // === Persist ===
        em.persist(persistentBuilding);
        em.persist(serverRoom);
        em.persist(area);
        em.persist(employee);
        em.persist(admin);

        em.flush(); // Γράψιμο στη βάση

        return card; // Return managed card
    }

    @Test
    @Transactional
    void testPersistPermission_LinksToCardAndArea() {
        String uniqueSuffix = generateUniqueId();
        AccessCard managedCard = createPersistentParents(uniqueSuffix);

        // Find Area (χρησιμοποιούμε το unique name)
        String areaName = "Test Zone Alpha" + uniqueSuffix;
        TypedQuery<Area> areaQuery = em.createQuery("select a from Area a where a.name = :name", Area.class);
        areaQuery.setParameter("name", areaName);
        Area managedArea = getFirstResult(areaQuery);

        // Create Permission
        Permission initialPermission = new Permission(PermissionType.AccessDenied, managedCard, managedArea);

        em.persist(initialPermission);
        em.flush();
        em.clear();

        // Retrieve and Verify
        Permission retrievedPermission = em.find(Permission.class, initialPermission.getPermissionId());

        assertNotNull(retrievedPermission, "Permission must be retrieved successfully.");
        assertEquals(managedCard.getCardId(), retrievedPermission.getAccessCard().getCardId(),
                "Must be linked to the correct AccessCard.");
        assertEquals(managedArea.getAreaId(), retrievedPermission.getArea().getAreaId(),
                "Must be linked to the correct Area.");
    }

    @Test
    @Transactional
    void testUpdatePermissionType_AndSecurityCheck() {
        String uniqueSuffix = generateUniqueId();
        AccessCard managedCard = createPersistentParents(uniqueSuffix);

        // Retrieve necessary objects dynamically based on the unique suffix
        String areaName = "Test Zone Alpha" + uniqueSuffix;
        Area managedArea = getFirstResult(em.createQuery("select a from Area a where a.name = :name", Area.class).setParameter("name", areaName));

        // Ψάχνουμε τον admin που φτιάξαμε ΣΕ ΑΥΤΟ το run
        User adminUser = getFirstResult(em.createQuery("select u from User u where u.username = :uname", User.class)
                .setParameter("uname", "Admin" + uniqueSuffix));

        // Ψάχνουμε τον employee που φτιάξαμε ΣΕ ΑΥΤΟ το run
        User employeeUser = getFirstResult(em.createQuery("select u from User u where u.username = :uname", User.class)
                .setParameter("uname", "User" + uniqueSuffix));

        // Create initial Permission
        Permission permission = new Permission(PermissionType.AccessDenied, managedCard, managedArea);
        em.persist(permission);
        em.flush();
        em.clear();

        // === Security Check (Failure) ===
        Permission managedPermission = em.find(Permission.class, permission.getPermissionId());

        assertThrows(SecurityException.class, () -> {
            managedPermission.updatePermissionType(PermissionType.AccessGranted, employeeUser);
        }, "Update must fail for a non-Administrator user.");

        em.clear();

        // === Security Check (Success) ===
        Permission managedPermissionForAdmin = em.find(Permission.class, permission.getPermissionId());

        // Χρειάζεται να ξαναβρούμε τον admin ως managed entity μετά το clear
        User managedAdmin = em.find(User.class, adminUser.getUserId());

        managedPermissionForAdmin.updatePermissionType(PermissionType.AccessGranted, managedAdmin);

        em.flush();
        em.clear();

        // Verify Update
        Permission finalPermission = em.find(Permission.class, permission.getPermissionId());
        assertEquals(PermissionType.AccessGranted, finalPermission.getAccessGranted(),
                "The PermissionType must be updated and persisted.");
    }

    @Test
    @Transactional
    void testDeletePermission_Success() {
        String uniqueSuffix = generateUniqueId();
        AccessCard managedCard = createPersistentParents(uniqueSuffix);

        String areaName = "Test Zone Alpha" + uniqueSuffix;
        Area managedArea = getFirstResult(em.createQuery("select a from Area a where a.name = :name", Area.class).setParameter("name", areaName));

        Permission permissionToDelete = new Permission(PermissionType.AccessDenied, managedCard, managedArea);
        em.persist(permissionToDelete);
        em.flush();

        int permissionId = permissionToDelete.getPermissionId();
        em.clear();

        // Delete
        Permission managedPermission = em.find(Permission.class, permissionId);
        assertNotNull(managedPermission, "Permission must be managed before deletion.");

        em.remove(managedPermission);
        em.flush();
        em.clear();

        // Verify
        assertNull(em.find(Permission.class, permissionId),
                "The Permission entity must be successfully deleted from the database.");
    }
}