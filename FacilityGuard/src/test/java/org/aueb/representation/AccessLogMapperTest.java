package org.aueb.representation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.persistence.*;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

@QuarkusTest
public class AccessLogMapperTest {

    @Inject
    AccessLogMapper accessLogMapper;

    @Inject
    AccessCardRepository cardRepo;

    @Inject
    CheckpointRepository checkpointRepo;

    @Inject
    AreaRepository areaRepo;

    @Inject
    BuildingRepository buildingRepo;

    /* ========= ENTITY → DTO ========= */

    @Test
    @Transactional
    void testEntityToRepresentation() {

        Building building = new Building("B", null);
        buildingRepo.persist(building);

        Area area = new Area("A", building);
        areaRepo.persist(area);

        Checkpoint checkpoint = new Checkpoint("CP");
        checkpoint.setArea(area);
        checkpointRepo.persist(checkpoint);

        AccessCard card = new AccessCard();
        card.setExpirationDate(new Date());   // <-- ΤΟ ΚΡΙΣΙΜΟ
        cardRepo.persist(card);


        AccessLog log = new AccessLog(
                PermissionType.AccessGranted,
                AccessType.In,
                card,
                checkpoint
        );

        AccessLogRepresentation dto =
                accessLogMapper.toRepresentation(log);

        Assertions.assertNotNull(dto);
        Assertions.assertEquals(card.getCardId(), dto.cardId);
        Assertions.assertEquals(checkpoint.getCheckpointId(), dto.checkpointId);
        Assertions.assertEquals(PermissionType.AccessGranted, dto.accessGranted);
        Assertions.assertEquals(AccessType.In, dto.accessType);
    }

    /* ========= DTO → ENTITY ========= */

    @Test
    @Transactional
    void testRepresentationToEntity() {

        Building building = new Building("B", null);
        buildingRepo.persist(building);

        Area area = new Area("A", building);
        areaRepo.persist(area);

        Checkpoint checkpoint = new Checkpoint("CP");
        checkpoint.setArea(area);
        checkpointRepo.persist(checkpoint);

        AccessCard card = new AccessCard();
        card.setExpirationDate(new Date());
        cardRepo.persist(card);

        AccessLogRepresentation dto = new AccessLogRepresentation();
        dto.cardId = card.getCardId();
        dto.checkpointId = checkpoint.getCheckpointId();
        dto.accessGranted = PermissionType.AccessDenied;
        dto.accessType = AccessType.Out;

        AccessLog entity = accessLogMapper.toModel(dto);

        Assertions.assertNotNull(entity);
        Assertions.assertEquals(card.getCardId(), entity.getAccessCard().getCardId());
        Assertions.assertEquals(checkpoint.getCheckpointId(), entity.getCheckpoint().getCheckpointId());
        Assertions.assertEquals(PermissionType.AccessDenied, entity.getAccessGranted());
        Assertions.assertEquals(AccessType.Out, entity.getAccessType());
    }

}
