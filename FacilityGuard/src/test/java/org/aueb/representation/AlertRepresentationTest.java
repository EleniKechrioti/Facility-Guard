package org.aueb.representation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.aueb.domain.AccessLog;
import org.aueb.domain.Alert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

@QuarkusTest
public class AlertRepresentationTest {

    @Inject
    AlertMapper alertMapper;

    @Test
    void testToRepresentation() {
        AccessLog log = new AccessLog();

        Alert alert = new Alert(new Date(), "Security Breach Detected");
        alert.setAccessLog(log);
        AlertRepresentation dto = alertMapper.toRepresentation(alert);

        Assertions.assertNotNull(dto);
        Assertions.assertEquals("Security Breach Detected", dto.message);
        Assertions.assertNotNull(dto.timestamp);

        Assertions.assertEquals(log.getLogId(), dto.accessLogId);
    }
}