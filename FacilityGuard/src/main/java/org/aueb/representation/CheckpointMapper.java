package org.aueb.representation;

import org.aueb.domain.Checkpoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta")
public interface CheckpointMapper {

    @Mapping(target = "areaId", source = "area.areaId")
    @Mapping(target = "permissionId", source = "permission.permissionId")
    CheckpointRepresentation toRepresentation(Checkpoint entity);
}
