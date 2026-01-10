package org.aueb.representation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.aueb.domain.Area;
import org.aueb.domain.Checkpoint;
import org.aueb.domain.Permission;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CheckpointRepresentationTest {

    @Inject
    CheckpointMapper checkpointMapper;

    @Test
    void testToRepresentation() {

        Area area = new Area();
        Permission permission = new Permission();

        Checkpoint checkpoint = new Checkpoint("Main Entrance");
        checkpoint.setArea(area);
        checkpoint.setPermission(permission);

        CheckpointRepresentation dto =
                checkpointMapper.toRepresentation(checkpoint);

        Assertions.assertNotNull(dto);
        Assertions.assertEquals("Main Entrance", dto.name);

        Assertions.assertEquals(
                checkpoint.getCheckpointId(),
                dto.checkpointId
        );

        Assertions.assertEquals(
                area.getAreaId(),
                dto.areaId
        );

        Assertions.assertEquals(
                permission.getPermissionId(),
                dto.permissionId
        );
    }
}
