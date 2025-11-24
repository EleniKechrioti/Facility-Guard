package org.aueb.domain;

import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    private User testUser;
    private User adminUser;
    private Date futureDate;

    @BeforeEach
    void setUp() {
        testUser = new User("tester", "p", "T", "T", "t@t.com", UserType.Employee);
        adminUser = new User("admin", "pass", "A", "A", "a@a.com", UserType.Administrator);
        futureDate = new Date(System.currentTimeMillis() + 86400000);
    }

    @Test
    void testSubmitRegistrationRequest_Success() {
        RegistrationRequest req = assertDoesNotThrow(() -> testUser.submitRegistrationRequest());

        assertNotNull(req, "Request object should be created.");
        assertEquals(ActivityStatus.Active, req.getStatus(), "New request status must be ACTIVE.");
        assertSame(testUser, req.getUser(), "Bidirectional link (Request -> User) must be correct.");
    }

    @Test
    void testSubmitRegistrationRequest_FailureActiveRequestExists() {
        testUser.submitRegistrationRequest();

        assertThrows(IllegalStateException.class, () -> testUser.submitRegistrationRequest(),
                "User should not be able to submit a second request while the first is ACTIVE.");
    }

    @Test
    void testIssueAccessCard_FullWorkflowSuccess() {
        RegistrationRequest req = testUser.submitRegistrationRequest();
        Date expiration = new Date(System.currentTimeMillis() + 86400000);

        req.setApprovedStatus(true, adminUser);

        AccessCard card = assertDoesNotThrow(() -> testUser.issueAccessCard(expiration));

        assertSame(card, testUser.getAccessCard(), "Card must be linked to User.");
        assertSame(testUser, card.getUser(), "Card must be linked to User (back-reference).");
    }

    @Test
    void testIssueAccessCard_NoApprovedRequestFailure() {
        testUser.submitRegistrationRequest();

        assertThrows(IllegalStateException.class, () -> testUser.issueAccessCard(futureDate),
                "Card issuance must fail if the request is not APPROVED (No ACTIVE and APPROVED request found).");
    }

    @Test
    void testIssueAccessCard_CardAlreadyExistsFailure() {
        RegistrationRequest request = testUser.submitRegistrationRequest();
        request.setApprovedStatus(true, adminUser);
        testUser.issueAccessCard(futureDate); // Issue first card successfully

        assertThrows(IllegalStateException.class, () -> {
            testUser.issueAccessCard(futureDate); // Attempt to issue second card
        }, "Should throw exception when user already has an access card.");
    }

    @Test
    void testSetAccessCard_BidirectionalCheck() {
        AccessCard newCard = new AccessCard(futureDate);

        testUser.setAccessCard(newCard);

        assertEquals(newCard, testUser.getAccessCard(), "The card must be set on the user.");
        assertEquals(testUser, newCard.getUser(), "The card must link back to the user (bidirectional link).");
    }

    @Test
    @DisplayName("[7] Correctly remove the assigned AccessCard and clear the bidirectional link")
    void testRemoveAccessCard_BidirectionalCheck() {
        AccessCard card = new AccessCard(futureDate);
        testUser.setAccessCard(card);

        testUser.removeAccessCard();

        assertNull(testUser.getAccessCard(), "The card must be removed from the user.");
        assertNull(card.getUser(), "The card must clear its reference to the user (bidirectional check).");
    }

    @Test
    void testRoleChecks_CorrectAssignment() {
        // Assert Employee
        assertTrue(testUser.isEmployee(), "Employee check must be true.");
        assertFalse(testUser.isAdmin(), "Employee check must be false for Admin role.");

        // Assert Admin
        assertTrue(adminUser.isAdmin(), "Admin check must be true.");
        assertFalse(adminUser.isEmployee(), "Admin check must be false for Employee role.");
    }

    @Test
    void testCheckPassword() {
        testUser.setPassword("MySecurePassword123");

        // Assert
        assertTrue(testUser.checkPassword("MySecurePassword123"), "Check must succeed with correct password.");
        assertFalse(testUser.checkPassword("WrongPassword"), "Check must fail with incorrect password.");
    }

    @Test
    void testCheckPassword_NullHash() {
        User nullPassUser = new User("user", null, "N", "P", "n@p.com", UserType.Employee);

        assertFalse(nullPassUser.checkPassword("AnyPassword"), "Must return false when the stored hash is null.");
    }
}