package org.aueb.domain;

import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = new User("tester", "p", "T", "T", "t@t.com", UserType.Employee);
        adminUser = new User("admin", "pass", "A", "A", "a@a.com", UserType.Administrator);
    }

    @Test
    void testSubmitRegistrationRequest_Success() {
        // 1. Υποβολή αιτήματος
        RegistrationRequest req = assertDoesNotThrow(() -> testUser.submitRegistrationRequest());

        // 2. Έλεγχος κατάστασης και σχέσης
        assertNotNull(req, "Request object should be created.");
        assertEquals(ActivityStatus.Active, req.getStatus(), "New request status must be ACTIVE.");
        assertTrue(testUser.getRegistrationRequests().contains(req), "User's collection must contain the request.");
        assertSame(testUser, req.getUser(), "Bidirectional link (Request -> User) must be correct.");
    }

    @Test
    void testSubmitRegistrationRequest_FailureActiveRequestExists() {
        // 1. Επιτυχής υποβολή του πρώτου αιτήματος (ACTIVE)
        testUser.submitRegistrationRequest();

        // 2. Δεύτερη υποβολή αποτυγχάνει λόγω ACTIVE status
        assertThrows(IllegalStateException.class, () -> testUser.submitRegistrationRequest(),
                "User should not be able to submit a second request while the first is ACTIVE.");
    }

    @Test
    void testIssueAccessCard_FullWorkflowSuccess() {
        // Setup: Υποβολή αιτήματος
        RegistrationRequest req = testUser.submitRegistrationRequest();

        // 1. Δοκιμή έκδοσης κάρτας πριν την έγκριση (αποτυγχάνει)
        Date expiration = new Date(System.currentTimeMillis() + 86400000);
        assertThrows(IllegalStateException.class, () -> testUser.issueAccessCard(expiration),
                "Card issuance must fail before the request is approved.");

        // 2. Έγκριση από Administrator
        req.setApprovedStatus(true, adminUser);

        // 3. Επιτυχής έκδοση κάρτας
        AccessCard card = assertDoesNotThrow(() -> testUser.issueAccessCard(expiration));

        // 4. Έλεγχος σχέσεων και κατάστασης
        assertNotNull(card, "AccessCard must be created.");
        assertSame(card, testUser.getAccessCard(), "Card must be linked to User (Owning Side).");
        assertSame(testUser, card.getUser(), "Card must be linked to User (Non-Owning Side).");
        assertEquals(ActivityStatus.Active, card.getStatus(), "New card must be ACTIVE.");

        // 5. Έλεγχος ότι το RegistrationRequest παραμένει ACTIVE
        assertEquals(ActivityStatus.Active, req.getStatus(),
                "Request status must remain ACTIVE after card issuance to block new requests.");

        // 6. Τρίτη υποβολή αποτυγχάνει ακόμα
        assertThrows(IllegalStateException.class, () -> testUser.submitRegistrationRequest(),
                "Cannot submit new request while the card is active (due to ACTIVE request status).");
    }
}
