package org.aueb.representation;

import org.aueb.domain.Checkpoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "jakarta")
public interface CheckpointMapper {

    @Mapping(target = "areaId", source = "area.areaId")
    @Mapping(target = "permissionId", source = "permission.permissionId")
    CheckpointRepresentation toRepresentation(Checkpoint entity);

    @Mapping(target = "area", ignore = true)
    @Mapping(target = "permission", ignore = true)
    Checkpoint toModel(CheckpointRepresentation dto);

    List<CheckpointRepresentation> toRepresentationList(List<Checkpoint> entityList);
}