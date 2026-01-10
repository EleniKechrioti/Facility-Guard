package org.aueb.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.aueb.domain.Area;
import org.aueb.domain.Building;
import org.aueb.domain.Checkpoint;
import org.aueb.persistence.AreaRepository;
import org.aueb.persistence.BuildingRepository;
import org.aueb.persistence.CheckpointRepository;
import org.aueb.representation.*;

import java.net.URI;
import java.util.List;

@Path("/buildings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BuildingResource {

    @Inject
    BuildingRepository buildingRepository;

    @Inject
    AreaRepository areaRepository;

    @Inject
    CheckpointRepository checkpointRepository;

    @Inject
    BuildingMapper buildingMapper;

    @Inject
    AreaMapper areaMapper;

    @Inject
    CheckpointMapper checkpointMapper;

    /**
     * GET /buildings
     * Returns a list of all buildings.
     */
    @GET
    public List<BuildingRepresentation> getAllBuildings() {
        return buildingMapper.toBuildingRepresentationList(buildingRepository.listAll());
    }

    /**
     * POST /buildings
     * It creates a new building.
     * Body: { "name": "...", "address": { ... } }
     */
    @POST
    @Transactional
    public Response createBuilding(@Valid BuildingRepresentation dto) {
        Building building = buildingMapper.toModel(dto);

        buildingRepository.persist(building);

        // Returns 201 Created with the new item
        return Response.created(URI.create("/buildings/" + building.getBuildingId()))
                .entity(buildingMapper.toRepresentation(building))
                .build();
    }

    /**
     * POST /buildings/{id}/areas
     * Adds a new zone (Area) to an existing building.
     * Body: { "name": "Server Room", "description": "..." }
     */
    @POST
    @Path("/{id}/areas")
    @Transactional
    public Response addAreaToBuilding(@PathParam("id") Integer buildingId, @Valid AreaRepresentation areaDto) { // <-- Προσθήκη @Valid
        // We find the building
        Building building = buildingRepository.findById(buildingId);

        if (building == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Convert DTO to Entity
        Area area = areaMapper.toModel(areaDto);

        // connect the objects
        building.addArea(area);

        // store
        areaRepository.persist(area);

        // return the answer (Entity -> DTO)
        return Response.created(URI.create("/buildings/" + buildingId + "/areas/" + area.getAreaId()))
                .entity(areaMapper.toRepresentation(area))
                .build();
    }

    /**
     * GET /buildings/{id}/areas
     * Returns all Areas of a specific building.
     */
    @GET
    @Path("/{id}/areas")
    public Response getAreasOfBuilding(@PathParam("id") Integer buildingId) {
        Building building = buildingRepository.findById(buildingId);
        if (building == null) return Response.status(404).build();

        var dtos = building.getAreas().stream()
                .map(area -> areaMapper.toRepresentation(area))
                .toList();

        return Response.ok(dtos).build();
    }


    /**
     * POST /buildings/{bid}/areas/{aid}/checkpoints
     * Add a Checkpoint to a Zone of a Building.
     */
    @POST
    @Path("/{buildingId}/areas/{areaId}/checkpoints")
    @Transactional
    public Response addCheckpointToArea(@PathParam("buildingId") Integer buildingId,
                                        @PathParam("areaId") Integer areaId,
                                        @Valid CheckpointRepresentation checkpointDto) {

        // Check if the building exists
        Building building = buildingRepository.findById(buildingId);
        if (building == null) return Response.status(Response.Status.NOT_FOUND).build();

        // Check if the area exists
        Area area = areaRepository.findById(areaId);
        if (area == null) return Response.status(Response.Status.NOT_FOUND).build();

        // Check that the zone actually belongs to this building
        if (area.getBuilding().getBuildingId() != buildingId) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Area does not belong to this building").build();
        }

        // Creating Checkpoints from DTO
        Checkpoint checkpoint = checkpointMapper.toModel(checkpointDto);

        // Connection to the area
        area.addCheckpoint(checkpoint);

        checkpointRepository.persist(checkpoint);

        return Response.created(URI.create("/buildings/" + buildingId + "/areas/" + areaId + "/checkpoints/" + checkpoint.getCheckpointId()))
                .entity(checkpointMapper.toRepresentation(checkpoint))
                .build();
    }

    /**
     * GET /buildings/{bid}/areas/{aid}/checkpoints
     * Download the Checkpoints of a Zone.
     */
    @GET
    @Path("/{buildingId}/areas/{areaId}/checkpoints")
    public Response getCheckpointsOfArea(@PathParam("buildingId") Integer buildingId,
                                         @PathParam("areaId") Integer areaId) {

        if (buildingRepository.findById(buildingId) == null) return Response.status(404).build();
        Area area = areaRepository.findById(areaId);
        if (area == null) return Response.status(404).build();

        if (area.getBuilding().getBuildingId() != buildingId) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Mismatch between Building and Area").build();
        }

        List<Checkpoint> checkpoints = checkpointRepository.fetchByArea(areaId);

        // Convert to DTO list
        List<CheckpointRepresentation> dtos = checkpointMapper.toRepresentationList(checkpoints);

        return Response.ok(dtos).build();
    }
}