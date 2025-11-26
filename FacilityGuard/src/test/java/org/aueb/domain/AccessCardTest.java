package org.aueb.domain;

import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

public class AccessCardTest {

    private AccessCard activeCard;

    @BeforeEach
    void setUp() {
        /** We create a card with an expiration date  */
        Date tomorrow = new Date(System.currentTimeMillis() + 86400000);
        activeCard = new AccessCard(tomorrow);
    }

    @Test
    void testConstructorSetsActiveStatus() {
        assertEquals(ActivityStatus.Active, activeCard.getStatus(),
                "New card should have ACTIVE status.");
        assertNotNull(activeCard.getExpirationDate(), "Expiration date must be set.");
    }

    @Test
    void testDeactivateCard_Success() {
        assertDoesNotThrow(() -> activeCard.deactivateCard());
        assertEquals(ActivityStatus.Inactive, activeCard.getStatus(),
                "Card status should be INACTIVE after deactivation.");
    }

    @Test
    void testDeactivateCard_FailureAlreadyInactive() {
        activeCard.deactivateCard();
        assertThrows(IllegalStateException.class, () -> activeCard.deactivateCard(),
                "Cannot deactivate an already INACTIVE card.");
    }

    @Test
    void testIsValid_LogicCoverage() {
        Date pastDate = new Date(System.currentTimeMillis() - 86400000);
        AccessCard expiredCard = new AccessCard(pastDate);

        assertTrue(activeCard.isValid(), "Card should be valid when status is ACTIVE and date is future.");

        assertFalse(expiredCard.isValid(), "Card should be invalid when expiration date is in the past.");

        activeCard.setStatus(ActivityStatus.Inactive);
        assertFalse(activeCard.isValid(), "Card should be invalid when status is INACTIVE.");
    }

    @Test
    void testReactivateCard_SuccessAndFailure() {
        AccessCard inactiveCard = new AccessCard();

        assertDoesNotThrow(() -> inactiveCard.reactivateCard());
        assertEquals(ActivityStatus.Active, inactiveCard.getStatus(), "Card status should be ACTIVE after reactivation.");

        assertThrows(IllegalStateException.class, () -> inactiveCard.reactivateCard(),
                "Cannot reactivate an already ACTIVE card.");
    }

    @Test
    void testSetUser_BidirectionalLink() {
        User user = new User("test", "p", "T", "T", "t@t.com", UserType.Employee);

        activeCard.setUser(user);

        assertEquals(user, activeCard.getUser(), "User link must be established.");
        assertSame(activeCard, user.getAccessCard(), "User's accessCard field must be set (bidirectional check).");
    }

    @Test
    void testAddPermission_BidirectionalIntegrity() {
        Permission p1 = new Permission();

        activeCard.addPermission(p1);

        assertEquals(1, activeCard.getPermissions().size(), "Permission must be added to the collection.");
        assertEquals(activeCard, p1.getAccessCard(), "Permission's accessCard must reference the correct card.");
    }

    @Test
    void testEqualityAndHashCode() {
        Date date = new Date(System.currentTimeMillis() + 1000);
        AccessCard card1 = new AccessCard(date);
        AccessCard card2 = new AccessCard(date);

        assertEquals(card1, card2, "Cards with the same ID must be equal.");

        assertEquals(card1.hashCode(), card2.hashCode(), "Hash codes must be equal if IDs are equal.");
    }
}