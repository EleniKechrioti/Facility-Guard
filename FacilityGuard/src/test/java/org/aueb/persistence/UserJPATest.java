package org.aueb.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the User entity, covering persistence,
 * relationships, constraints, and domain logic persistence.
 */
@QuarkusTest
public class UserJPATest extends JPATest {

    /** Helper Method to Create a Valid User */
    private User createTestUser(String username, String email) {
        return new User(username, "Password123", "Test", "User", email, UserType.Employee);
    }

    /**
     * Helper Method to Create a RegistrationRequest STUB.
     * Πλέον δεν ανοίγει δικά του transactions, βασίζεται στο @Transactional του test.
     */
    private RegistrationRequest createAndPersistApprovedRequest(User user) {
        // We create a User Admin for approval (if it doesn't already exist)
        User adminUser = new User("temp_admin_" + System.currentTimeMillis(), "pass", "admin", "admin", "admin@a.com", UserType.Administrator);
        em.persist(adminUser);

        // We find the managed user
        User managedUser = em.find(User.class, user.getUserId());

        RegistrationRequest req = new RegistrationRequest();
        managedUser.addRegistrationRequest(req);

        em.persist(req);

        // We send to the base to get ID
        em.flush();

        // Approve
        req.setApprovedStatus(true, adminUser);

        // We resend the update
        em.flush();
        em.clear(); // We clean to return fresh item

        return em.find(RegistrationRequest.class, req.getRegistrationId());
    }

    @Test
    @Transactional
    public void listAllUsers(){
        List<User> result = em.createQuery("select u from User u", User.class).getResultList();
        assertEquals(3, result.size(), "Should verify the user from import.sql");
    }

    /**
     * Tests the successful creation and retrieval of a User, and verifies the
     * UNIQUE constraint on the username field.
     */
    @Test
    @Transactional
    void testPersistAndRetrieveUser() {
        /** Arrange: Create a new User   */
        User user1 = createTestUser("user_a", "a@test.com");
        User user2 = createTestUser("user_a", "b@test.com"); // Duplicate username

        em.persist(user1);
        em.flush();
        em.clear();

        User retrievedUser = em.find(User.class, user1.getUserId());
        assertNotNull(retrievedUser, "User must be retrieved successfully.");
        assertFalse(retrievedUser.getPassword().equals("Password123"), "Password must be hashed.");

        // Unique Constraint Test
        // We try to save user2 with the same username
        em.persist(user2);

        assertThrows(PersistenceException.class, () -> {
            em.flush(); // Εδώ θα σκάσει η βάση λόγω unique constraint
        }, "Persisting duplicate username must throw PersistenceException.");
    }

    /**
     * Tests the cascade persistence and deletion of the One-to-One AccessCard relationship.
     */
    @Test
    @Transactional
    void testAccessCard() {
        User user = createTestUser("user_card", "card@test.com");
        AccessCard card = new AccessCard(new Date());

        user.setAccessCard(card);

        em.persist(user); // Cascades to AccessCard
        em.flush();
        em.clear();

        // We need to find the ID again because we made clear
        User managedUser = em.createQuery("select u from User u where u.email = 'card@test.com'", User.class).getSingleResult();
        int cardId = managedUser.getAccessCard().getCardId();

        assertNotNull(em.find(AccessCard.class, cardId), "AccessCard must be persisted via cascade.");

        // Deletion
        em.remove(managedUser);
        em.flush(); // Run delete
        em.clear();

        assertNull(em.find(AccessCard.class, cardId), "AccessCard must be deleted via cascade.");
    }

    /**
     * Tests the orphanRemoval=true setting.
     */
    @Test
    @Transactional
    void testRegistrationRequest() {
        User user = createTestUser("user_req", "req@test.com");
        RegistrationRequest req1 = new RegistrationRequest();
        user.addRegistrationRequest(req1);

        em.persist(user);
        em.flush();
        em.clear();

        // Recover the user and Request ID
        User managedUser = em.createQuery("select u from User u where u.email = 'req@test.com'", User.class).getSingleResult();
        int requestId = managedUser.getRegistrationRequests().iterator().next().getRegistrationId();

        // Delete the Request from the collection (orphan removal)
        managedUser.getRegistrationRequests().clear();

        em.flush(); // This will send the delete statement to the base
        em.clear();

        assertNull(em.find(RegistrationRequest.class, requestId), "Request must be deleted due to orphanRemoval.");
    }

    /**
     * Tests the critical domain flow: submit -> approve -> issue card
     */
    @Test
    @Transactional
    void testDomainFlow() {
        User user = createTestUser("user_flow", "flow@test.com");
        em.persist(user);
        em.flush(); // The user must be present in the base for the helper to find him

        // Helper does flush/clear himself
        createAndPersistApprovedRequest(user);

        // User reload (because the helper cleared)
        User managedUser = em.find(User.class, user.getUserId());
        Date expiration = new Date();

        // Card Issuance
        managedUser.issueAccessCard(expiration);

        em.flush();
        em.clear();

        User finalUser = em.find(User.class, user.getUserId());

        assertNotNull(finalUser.getAccessCard(), "Access Card must be created and persisted.");
        assertEquals(finalUser.getUserId(), finalUser.getAccessCard().getUser().getUserId(), "Card must link back to the user.");
    }

    /**
     * Verifies password hashing persistence.
     */
    @Test
    @Transactional
    void testPasswordHashing() {
        User user = createTestUser("user_hash", "hash@test.com");
        user.setPassword("SecretPlaintext");

        em.persist(user);
        em.flush();
        em.clear();

        User retrievedUser = em.find(User.class, user.getUserId());

        String storedPassword = retrievedUser.getPassword();
        assertFalse(storedPassword.equals("SecretPlaintext"), "Stored password must not be plain text.");

        assertTrue(retrievedUser.checkPassword("SecretPlaintext"), "checkPassword must validate the original text.");
    }
}