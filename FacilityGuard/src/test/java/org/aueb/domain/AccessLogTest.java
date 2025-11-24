package org.aueb.domain;

import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests για την κλάση AccessLog (χωρίς ΒΔ).
 */
public class AccessLogTest {

    @Test
    void createAccessLog_setsFieldsCorrectly_andLinksToCardAndCheckpoint() {
        // Arrange
        Date exp = new Date();
        AccessCard card = new AccessCard(exp);
        Checkpoint checkpoint = new Checkpoint("Main Gate");

        // Act
        AccessLog log = new AccessLog(
                PermissionType.AccessGranted,
                AccessType.In,
                card,
                checkpoint
        );

        // Assert βασικών πεδίων
        assertNotNull(log.getTimestamp(), "Timestamp must be set.");
        assertEquals(PermissionType.AccessGranted, log.getAccessGranted());
        assertEquals(AccessType.In, log.getAccessType());

        // Assert σχέσεων
        assertEquals(card, log.getAccessCard(), "AccessLog must reference the AccessCard.");
        assertEquals(checkpoint, log.getCheckpoint(), "AccessLog must reference the Checkpoint.");

        // Ελέγχουμε και την αμφίδρομη σχέση με την κάρτα
        assertTrue(card.getAccessLogs().contains(log),
                "AccessCard must contain the AccessLog in its collection.");
    }

    @Test
    void changeAccessDecision_updatesEnumField() {
        Date exp = new Date();
        AccessCard card = new AccessCard(exp);
        Checkpoint cp = new Checkpoint("Server Room");

        AccessLog log = new AccessLog(
                PermissionType.AccessDenied,
                AccessType.In,
                card,
                cp
        );

        log.setAccessGranted(PermissionType.AccessGranted);

        assertEquals(PermissionType.AccessGranted, log.getAccessGranted(),
                "AccessGranted must be updated.");
    }
}
