package org.aueb.representation;

import org.aueb.domain.Area;
import org.aueb.domain.Checkpoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "jakarta")
public interface AreaMapper {

    // Entity -> DTO
    @Mapping(source = "areaId", target = "id")
    @Mapping(source = "building.buildingId", target = "buildingId")
    @Mapping(source = "neighbors", target = "neighborIds", qualifiedByName = "mapNeighborsToIds")
    @Mapping(source = "checkpoints", target = "checkpointNames", qualifiedByName = "mapCheckpointsToNames")
    AreaRepresentation toRepresentation(Area area);

    // DTO -> Entity
    @Mapping(source = "id", target = "areaId")
    @Mapping(target = "building", ignore = true)     // We define the building in Resource
    @Mapping(target = "neighbors", ignore = true)
    @Mapping(target = "checkpoints", ignore = true)  // We add the checkpoints separately
    Area toModel(AreaRepresentation representation);

    // --- Custom Mapping Methods ---
    // We need these methods to convert Objects into simple IDs/Strings
    // so that we don't have Infinite Recursion (Area -> Neighbor -> Area -> Neighbor...)

    @Named("mapNeighborsToIds")
    default Set<Integer> mapNeighborsToIds(Set<Area> neighbors) {
        if (neighbors == null) return null;
        return neighbors.stream()
                .map(Area::getAreaId)
                .collect(Collectors.toSet());
    }

    @Named("mapCheckpointsToNames")
    default Set<String> mapCheckpointsToNames(Set<Checkpoint> checkpoints) {
        if (checkpoints == null) return null;
        return checkpoints.stream()
                .map(Checkpoint::getName)
                .collect(Collectors.toSet());
    }
}