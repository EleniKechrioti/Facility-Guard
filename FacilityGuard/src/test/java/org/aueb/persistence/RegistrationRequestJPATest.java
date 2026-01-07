package org.aueb.persistence;

import jakarta.persistence.EntityManager;
import org.aueb.domain.RegistrationRequest;
import org.aueb.domain.User;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RegistrationRequestJPATest {

    private EntityManager em;

    @Test
    void testPersistRegistrationRequest() {

        // === Create administrator ===
        em.getTransaction().begin();
        User admin = new User(
                "adminUser",
                "pass",
                "Admin",
                "User",
                "admin@test.com",
                UserType.Administrator
        );
        em.persist(admin);
        em.getTransaction().commit();
        em.clear();

        // === Create RegistrationRequest ===
        RegistrationRequest req = new RegistrationRequest();
        req.setUser(em.find(User.class, admin.getUserId()));

        em.getTransaction().begin();
        em.persist(req);
        em.getTransaction().commit();
        em.clear();

        // === Retrieve ===
        RegistrationRequest saved =
                em.find(RegistrationRequest.class, req.getRegistrationId());

        assertNotNull(saved);
        assertEquals(ActivityStatus.Active, saved.getStatus());
        assertFalse(saved.isApproved());
        assertNotNull(saved.getRequestDate());

        assertEquals(admin.getUserId(), saved.getUser().getUserId());
    }

    @Test
    void testUpdateApprovedStatus_Approve() {

        // === Setup admin + request ===
        em.getTransaction().begin();
        User admin = new User("admin2", "pass", "A", "A", "a@a.com", UserType.Administrator);
        em.persist(admin);

        RegistrationRequest req = new RegistrationRequest();
        req.setUser(admin);
        em.persist(req);

        em.getTransaction().commit();
        em.clear();

        // === Approve ===
        RegistrationRequest managedReq =
                em.find(RegistrationRequest.class, req.getRegistrationId());

        em.getTransaction().begin();
        managedReq.setApprovedStatus(true, admin);
        em.getTransaction().commit();
        em.clear();

        RegistrationRequest updated =
                em.find(RegistrationRequest.class, req.getRegistrationId());

        assertTrue(updated.isApproved());
        assertEquals(ActivityStatus.Active, updated.getStatus());
    }

    @Test
    void testUpdateApprovedStatus_Reject() {

        em.getTransaction().begin();
        User admin = new User("admin3", "pass", "A", "A", "admin3@mail.com", UserType.Administrator);
        em.persist(admin);

        RegistrationRequest req = new RegistrationRequest();
        req.setUser(admin);
        em.persist(req);

        em.getTransaction().commit();
        em.clear();

        // === Reject ===
        RegistrationRequest managedReq =
                em.find(RegistrationRequest.class, req.getRegistrationId());

        em.getTransaction().begin();
        managedReq.setApprovedStatus(false, admin);
        em.getTransaction().commit();
        em.clear();

        RegistrationRequest updated =
                em.find(RegistrationRequest.class, req.getRegistrationId());

        assertFalse(updated.isApproved());
        assertEquals(ActivityStatus.Inactive, updated.getStatus());
    }

    @Test
    void testInvalidateRequest() {

        em.getTransaction().begin();

        User admin = new User("admin4", "pass", "A", "A", "admin4@mail.com", UserType.Administrator);
        em.persist(admin);

        RegistrationRequest req = new RegistrationRequest();
        req.setUser(admin);
        em.persist(req);

        em.getTransaction().commit();
        em.clear();

        // === Invalidate ===
        RegistrationRequest managedReq =
                em.find(RegistrationRequest.class, req.getRegistrationId());

        em.getTransaction().begin();
        managedReq.invalidateRequest(admin);
        em.getTransaction().commit();
        em.clear();

        RegistrationRequest updated =
                em.find(RegistrationRequest.class, req.getRegistrationId());

        assertEquals(ActivityStatus.Inactive, updated.getStatus());
        assertFalse(updated.isApproved());
    }
}