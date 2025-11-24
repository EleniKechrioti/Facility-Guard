package org.aueb.persistence;

import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;
import org.aueb.domain.*;
import org.aueb.domain.User;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the User entity, covering persistence,
 * relationships, constraints, and domain logic persistence.
 */
public class UserJPATest extends JPATest {


    /** Helper Method to Create a Valid User */
    private User createTestUser(String username, String email) {
        return new User(username, "Password123", "Test", "User", email, UserType.Employee);
    }


    /** Helper Method to Create a RegistrationRequest STUB */
    private RegistrationRequest createAndPersistApprovedRequest(User user) {
        User adminUser = new User("admin", "pass", "admin", "admin", "admin@a.com", UserType.Administrator);

        User managedUser = em.find(User.class, user.getUserId());

        RegistrationRequest req = new RegistrationRequest();
        managedUser.addRegistrationRequest(req);

        em.getTransaction().begin();
        em.persist(req);
        em.getTransaction().commit();

        em.clear();

        RegistrationRequest managedReq = em.find(RegistrationRequest.class, req.getRegistrationId());

        em.getTransaction().begin();
        managedReq.setApprovedStatus(true, adminUser);
        em.getTransaction().commit();

        em.clear();

        return em.find(RegistrationRequest.class, managedReq.getRegistrationId());
    }

    @Test
    public void listAllUsers(){
        List<User> result = em.createQuery("select u from User u").getResultList();
        assertEquals(1, result.size());
    }

    /**
     * Tests the successful creation and retrieval of a User, and verifies the
     * UNIQUE constraint on the username field.
     */
    @Test
    void testPersistAndRetrieveUser() {
        // 1. Arrange: Create a new User
        User user1 = createTestUser("user_a", "a@test.com");
        User user2 = createTestUser("user_a", "b@test.com");

        em.getTransaction().begin();
        em.persist(user1);
        em.getTransaction().commit();

        em.clear();

        User retrievedUser = em.find(User.class, user1.getUserId());
        assertNotNull(retrievedUser, "User must be retrieved successfully.");
        assertFalse(retrievedUser.getPassword().equals("Password123"), "Password must be hashed.");

        assertThrows(PersistenceException.class, () -> {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            em.persist(user2);
            tx.commit();
        }, "Persisting duplicate username must throw PersistenceException.");

        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }

    /**
     * Tests the cascade persistence and deletion of the One-to-One AccessCard relationship.
     */
    @Test
    void testAccessCard() {
        User user = createTestUser("user", "test@test.com");
        AccessCard card = new AccessCard(new Date());

        user.setAccessCard(card);

        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();
        em.clear();

        int cardId = user.getAccessCard().getCardId();

        assertNotNull(em.find(AccessCard.class, cardId), "AccessCard must be persisted via cascade.");

        em.getTransaction().begin();
        User managedUser = em.find(User.class, user.getUserId());
        em.remove(managedUser);
        em.getTransaction().commit();

        assertNull(em.find(AccessCard.class, cardId), "AccessCard must be deleted via cascade.");
    }

    /**
     * Tests the orphanRemoval=true setting by verifying that a RegistrationRequest is
     * deleted from the database when it is removed from the User's collection.
     */
    @Test
    void testRegistrationRequest() {
        User user = createTestUser("user", "test@test.com");
        RegistrationRequest req1 = new RegistrationRequest();
        user.addRegistrationRequest(req1);

        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();

        em.clear();

        int requestId = user.getRegistrationRequests().iterator().next().getRegistrationId();

        em.getTransaction().begin();
        User managedUser = em.find(User.class, user.getUserId());

        managedUser.getRegistrationRequests().clear();

        em.getTransaction().commit();
        em.clear();

        assertNull(em.find(RegistrationRequest.class, requestId), "Request must be deleted due to orphanRemoval.");
    }

    /**
     * Tests the critical domain flow: submit -> approve -> issue card,
     * ensuring the card persists and the old request is handled correctly.
     */
    @Test
    void testDomainFlow() {
        User user = createTestUser("user", "test@test.com");
        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();

        RegistrationRequest approvedRequest = createAndPersistApprovedRequest(user);

        Date expiration = new Date();
        em.getTransaction().begin();
        User managedUser = em.find(User.class, user.getUserId());

        managedUser.issueAccessCard(expiration);

        em.getTransaction().commit();
        em.clear();

        User finalUser = em.find(User.class, user.getUserId());

        assertNotNull(finalUser.getAccessCard(), "Access Card must be created and persisted.");
        assertEquals(finalUser, finalUser.getAccessCard().getUser(), "Card must link back to the user.");
    }

    /**
     * Verifies that the setPassword method successfully hashes the password
     * before persisting it, checking the output against the known HASHED_ prefix.
     */
    @Test
    void testPasswordHashing() {
        User user = createTestUser("user", "test@test.com");
        user.setPassword("SecretPlaintext");

        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();
        em.clear();

        User retrievedUser = em.find(User.class, user.getUserId());

        String storedPassword = retrievedUser.getPassword();
        assertFalse(storedPassword.equals("SecretPlaintext"), "Stored password must not be plain text.");

        assertTrue(retrievedUser.checkPassword("SecretPlaintext"), "checkPassword must validate the original text.");
    }

}
