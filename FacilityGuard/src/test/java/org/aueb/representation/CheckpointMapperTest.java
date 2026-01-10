package org.aueb.representation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.aueb.domain.Area;
import org.aueb.domain.Checkpoint;
import org.aueb.domain.Permission;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CheckpointMapperTest {

    @Inject
    CheckpointMapper checkpointMapper;

    /* ========= ENTITY → DTO ========= */

    @Test
    void testEntityToRepresentation() {

        // dummy Area
        Area area = new Area();
        area.setAreaId(10);

        // dummy Permission (ΧΩΡΙΣ id)
        Permission permission = new Permission();

        Checkpoint checkpoint = new Checkpoint("Main Gate");
        checkpoint.setArea(area);
        checkpoint.setPermission(permission);

        CheckpointRepresentation dto =
                checkpointMapper.toRepresentation(checkpoint);

        Assertions.assertNotNull(dto);
        Assertions.assertEquals("Main Gate", dto.name);
        Assertions.assertEquals(10, dto.areaId);

        // permissionId ΔΕΝ υπάρχει (δεν έχει γίνει persist)
        Assertions.assertEquals(0, dto.permissionId);
    }

    /* ========= DTO → ENTITY ========= */

    @Test
    void testRepresentationToEntity() {

        CheckpointRepresentation dto = new CheckpointRepresentation();
        dto.name = "Back Door";
        dto.areaId = 99;
        dto.permissionId = 88;

        Checkpoint entity =
                checkpointMapper.toModel(dto);

        Assertions.assertNotNull(entity);
        Assertions.assertEquals("Back Door", entity.getName());

        // mapper ignores relations
        Assertions.assertNull(entity.getArea());
        Assertions.assertNull(entity.getPermission());
    }
}
