package org.aueb.representation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.aueb.domain.AccessCard;
import org.aueb.domain.AccessLog;
import org.aueb.domain.Checkpoint;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AccessLogRepresentationTest {

    @Inject
    AccessLogMapper accessLogMapper;

    @Test
    void testToRepresentation() {

        AccessCard card = new AccessCard();
        Checkpoint checkpoint = new Checkpoint();

        AccessLog log = new AccessLog();
        log.setAccessGranted(PermissionType.AccessGranted);
        log.setAccessType(AccessType.In);
        log.setAccessCard(card);
        log.setCheckpoint(checkpoint);

        AccessLogRepresentation dto =
                accessLogMapper.toRepresentation(log);

        Assertions.assertNotNull(dto);
        Assertions.assertEquals(PermissionType.AccessGranted, dto.accessGranted);
        Assertions.assertEquals(AccessType.In, dto.accessType);
        Assertions.assertEquals(card.getCardId(), dto.cardId);
        Assertions.assertEquals(checkpoint.getCheckpointId(), dto.checkpointId);
    }
}
