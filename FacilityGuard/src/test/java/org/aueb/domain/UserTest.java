package org.aueb.domain;

import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    private User testUser;
    private User adminUser;
    private User visitor;
    private Date futureDate;

    @BeforeEach
    void setUp() {
        testUser = new User("tester", "p", "T", "T", "t@t.com", UserType.Employee);
        adminUser = new User("admin", "pass", "A", "A", "a@a.com", UserType.Administrator);
        visitor = new User("vis", "p", "V", "V", "v@v.com", UserType.Visitor);
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
        testUser.issueAccessCard(futureDate); /** Issue first card successfully  */

        assertThrows(IllegalStateException.class, () -> {
            testUser.issueAccessCard(futureDate); /** Attempt to issue second card  */
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
    void testRemoveAccessCard_BidirectionalCheck() {
        AccessCard card = new AccessCard(futureDate);
        testUser.setAccessCard(card);

        testUser.removeAccessCard();

        assertNull(testUser.getAccessCard(), "The card must be removed from the user.");
        assertNull(card.getUser(), "The card must clear its reference to the user (bidirectional check).");
    }

    @Test
    void testSetAccessCard_SkipUpdate() {
        AccessCard card = new AccessCard(futureDate);

        testUser.setAccessCard(card);

        card.setUser(testUser);

        assertDoesNotThrow(() -> testUser.setAccessCard(card),
                "Should execute without error.");

        assertEquals(testUser, card.getUser(), "Η σχέση πρέπει να παραμείνει αμετάβλητη.");
    }

    @Test
    void testUserEquality() {
        // Arrange
        User userA = new User("user_a", "p", "T", "T", "a@a.com", UserType.Employee);
        User userB = new User("user_b", "p", "T", "T", "b@b.com", UserType.Employee);

        assertNotEquals(userA, userB, "Objects with different references must be unequal.");

        assertEquals(testUser, testUser, "An object must equal itself.");
        assertNotEquals(testUser, null, "Object must not equal null.");

    }

    @Test
    void testRoleChecks_CorrectAssignment() {
        /** Assert Employee   */
        assertTrue(testUser.isEmployee(), "Employee check must be true.");
        assertFalse(testUser.isAdmin(), "Employee check must be false for Admin role.");

        /** Assert Admin   */
        assertTrue(adminUser.isAdmin(), "Admin check must be true.");
        assertFalse(adminUser.isEmployee(), "Admin check must be false for Employee role.");

        assertTrue(visitor.isVisitor(), "Visitor check must be true.");
        assertFalse(visitor.isAdmin(), "Visitor must not be Admin.");
    }

    @Test
    void testCheckPassword() {
        testUser.setPassword("MySecurePassword123");

        /** Assert   */
        assertTrue(testUser.checkPassword("MySecurePassword123"), "Check must succeed with correct password.");
        assertFalse(testUser.checkPassword("WrongPassword"), "Check must fail with incorrect password.");
    }

    @Test
    void testCheckPassword_NullHash() {
        User nullPassUser = new User("user", null, "N", "P", "n@p.com", UserType.Employee);

        assertFalse(nullPassUser.checkPassword("AnyPassword"), "Must return false when the stored hash is null.");
    }
}