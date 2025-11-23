package org.aueb.domain;

import org.aueb.util.enumerations.ActivityStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

public class AccessCardTest {

    private AccessCard activeCard;

    @BeforeEach
    void setUp() {
        // Δημιουργούμε μια κάρτα με ημερομηνία λήξης
        Date tomorrow = new Date(System.currentTimeMillis() + 86400000);
        activeCard = new AccessCard(tomorrow); // Ξεκινάει ACTIVE
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
        activeCard.deactivateCard(); // Πρώτη απενεργοποίηση επιτυχής

        // Δεύτερη απενεργοποίηση αποτυγχάνει
        assertThrows(IllegalStateException.class, () -> activeCard.deactivateCard(),
                "Cannot deactivate an already INACTIVE card.");
    }
}