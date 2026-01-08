package org.aueb.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.aueb.domain.RegistrationRequest;
import org.aueb.domain.User;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class RegistrationRequestJPATest {

    @Inject
    EntityManager em;

    @Test
    @Transactional
    void testPersistRegistrationRequest() {
        // === Create administrator ===
        User admin = new User(
                "adminUser",
                "pass",
                "Admin",
                "User",
                "admin@test.com",
                UserType.Administrator
        );
        em.persist(admin);

        // === Create RegistrationRequest ===
        RegistrationRequest req = new RegistrationRequest();
        req.setUser(admin);
        em.persist(req);

        // Force write and clear cache to simulate retrieval from DB
        em.flush();
        em.clear();

        // === Retrieve ===
        RegistrationRequest saved = em.find(RegistrationRequest.class, req.getRegistrationId());

        assertNotNull(saved);
        assertEquals(ActivityStatus.Active, saved.getStatus());
        assertFalse(saved.isApproved());
        assertNotNull(saved.getRequestDate());

        // Ελέγχουμε αν συνδέθηκε σωστά με τον user
        assertNotNull(saved.getUser());
        assertEquals(admin.getUserId(), saved.getUser().getUserId());
    }

    @Test
    @Transactional
    void testUpdateApprovedStatus_Approve() {
        // === Setup admin + request ===
        User admin = new User("admin2", "pass", "A", "A", "a@a.com", UserType.Administrator);
        em.persist(admin);

        RegistrationRequest req = new RegistrationRequest();
        req.setUser(admin);
        em.persist(req);

        em.flush();
        em.clear();

        // === Approve ===
        RegistrationRequest managedReq = em.find(RegistrationRequest.class, req.getRegistrationId());
        User managedAdmin = em.find(User.class, admin.getUserId());

        managedReq.setApprovedStatus(true, managedAdmin);

        em.flush();
        em.clear();

        // === Verify ===
        RegistrationRequest updated = em.find(RegistrationRequest.class, req.getRegistrationId());
        assertTrue(updated.isApproved());
        assertEquals(ActivityStatus.Active, updated.getStatus());
    }

    @Test
    @Transactional
    void testUpdateApprovedStatus_Reject() {
        // === Setup ===
        User admin = new User("admin3", "pass", "A", "A", "admin3@mail.com", UserType.Administrator);
        em.persist(admin);

        RegistrationRequest req = new RegistrationRequest();
        req.setUser(admin);
        em.persist(req);

        em.flush();
        em.clear();

        // === Reject ===
        RegistrationRequest managedReq = em.find(RegistrationRequest.class, req.getRegistrationId());
        User managedAdmin = em.find(User.class, admin.getUserId());

        managedReq.setApprovedStatus(false, managedAdmin);

        em.flush();
        em.clear();

        // === Verify ===
        RegistrationRequest updated = em.find(RegistrationRequest.class, req.getRegistrationId());
        assertFalse(updated.isApproved());
        assertEquals(ActivityStatus.Inactive, updated.getStatus());
    }

    @Test
    @Transactional
    void testInvalidateRequest() {
        // === Setup ===
        User admin = new User("admin4", "pass", "A", "A", "admin4@mail.com", UserType.Administrator);
        em.persist(admin);

        RegistrationRequest req = new RegistrationRequest();
        req.setUser(admin);
        em.persist(req);

        em.flush();
        em.clear();

        // === Invalidate ===
        RegistrationRequest managedReq = em.find(RegistrationRequest.class, req.getRegistrationId());
        User managedAdmin = em.find(User.class, admin.getUserId());

        managedReq.invalidateRequest(managedAdmin);

        em.flush();
        em.clear();

        // === Verify ===
        RegistrationRequest updated = em.find(RegistrationRequest.class, req.getRegistrationId());
        assertEquals(ActivityStatus.Inactive, updated.getStatus());
        assertFalse(updated.isApproved());
    }
}