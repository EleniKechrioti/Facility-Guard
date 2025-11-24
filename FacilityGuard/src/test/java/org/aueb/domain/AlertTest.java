package org.aueb.domain;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class AlertTest {

    @Test
    void testAlertConstructorAndFields() {
        Date now = new Date();
        String msg = "Unauthorized access attempt!";

        Alert alert = new Alert(now, msg);

        assertEquals(now, alert.getTimestamp());
        assertEquals(msg, alert.getMessage());
    }

    @Test
    void testAlertToString() {
        Alert alert = new Alert(new Date(), "Test alert");
        String s = alert.toString();
        assertTrue(s.contains("Alert"));
        assertTrue(s.contains("message"));
    }

    @Test
    void testAlertLinkedToAccessLog() {
        AccessCard card = new AccessCard(new Date());
        Checkpoint cp = new Checkpoint("Gate A");

        AccessLog log = new AccessLog(
                org.aueb.util.enumerations.PermissionType.AccessDenied,
                org.aueb.util.enumerations.AccessType.In,
                card,
                cp
        );

        Alert alert = new Alert(new Date(), "Violation");
        alert.setAccessLog(log);

        assertEquals(log, alert.getAccessLog());
    }
}
