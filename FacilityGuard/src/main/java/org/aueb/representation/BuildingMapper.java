package org.aueb.representation;

import org.aueb.domain.Building;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "jakarta", uses = AreaMapper.class)
public interface BuildingMapper {

    // Building Mappings
    @Mapping(source = "buildingId", target = "id")
    BuildingRepresentation toRepresentation(Building building);

    @Mapping(source = "id", target = "buildingId")
    @Mapping(target = "areas", ignore = true)
    Building toModel(BuildingRepresentation representation);

    List<BuildingRepresentation> toBuildingRepresentationList(List<Building> buildings);
}