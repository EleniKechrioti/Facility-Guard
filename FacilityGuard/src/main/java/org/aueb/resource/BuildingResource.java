package org.aueb.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.aueb.domain.Area;
import org.aueb.domain.Building;
import org.aueb.domain.Checkpoint;
import org.aueb.representation.*;
import org.aueb.service.FacilityService;

import java.net.URI;
import java.util.List;

@Path("/buildings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BuildingResource {

    @Inject
    FacilityService facilityService;

    @Inject BuildingMapper buildingMapper;
    @Inject AreaMapper areaMapper;
    @Inject CheckpointMapper checkpointMapper;

    /**
     * GET /buildings
     * Returns a list of all buildings.
     */
    @GET
    public List<BuildingRepresentation> getAllBuildings() {
        return buildingMapper.toBuildingRepresentationList(facilityService.getAllBuildings());
    }

    /**
     * POST /buildings
     * It creates a new building.
     * Body: { "name": "...", "address": { ... } }
     */
    @POST
    public Response createBuilding(@Valid BuildingRepresentation dto) {
        // DTO -> Entity
        Building building = buildingMapper.toModel(dto);

        // Business Logic
        facilityService.createBuilding(building);

        // Entity -> DTO
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
    public Response addAreaToBuilding(@PathParam("id") Integer buildingId, @Valid AreaRepresentation areaDto) {
        try {
            // DTO -> Entity
            Area area = areaMapper.toModel(areaDto);

            // Call Service
            facilityService.addAreaToBuilding(buildingId, area);

            // Return Response
            return Response.created(URI.create("/buildings/" + buildingId + "/areas/" + area.getAreaId()))
                    .entity(areaMapper.toRepresentation(area))
                    .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * GET /buildings/{id}/areas
     * Returns all Areas of a specific building.
     */
    @GET
    @Path("/{id}/areas")
    public Response getAreasOfBuilding(@PathParam("id") Integer buildingId) {
        Building building = facilityService.findBuildingById(buildingId);

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
    public Response addCheckpointToArea(@PathParam("buildingId") Integer buildingId,
                                        @PathParam("areaId") Integer areaId,
                                        @Valid CheckpointRepresentation checkpointDto) {
        try {
            // DTO -> Entity
            Checkpoint checkpoint = checkpointMapper.toModel(checkpointDto);

            // Call Service
            facilityService.addCheckpointToArea(buildingId, areaId, checkpoint);

            // Return Response
            return Response.created(URI.create("/buildings/" + buildingId + "/areas/" + areaId + "/checkpoints/" + checkpoint.getCheckpointId()))
                    .entity(checkpointMapper.toRepresentation(checkpoint))
                    .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    /**
     * GET /buildings/{bid}/areas/{aid}/checkpoints
     */
    @GET
    @Path("/{buildingId}/areas/{areaId}/checkpoints")
    public Response getCheckpointsOfArea(@PathParam("buildingId") Integer buildingId,
                                         @PathParam("areaId") Integer areaId) {

        Building building = facilityService.findBuildingById(buildingId);
        Area area = facilityService.findAreaById(areaId);

        if (building == null || area == null) return Response.status(404).build();

        if (area.getBuilding().getBuildingId() != buildingId) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Mismatch between Building and Area").build();
        }

        // Fetch via Service or Repository wrapper
        List<Checkpoint> checkpoints = facilityService.getCheckpointsOfArea(areaId);

        return Response.ok(checkpointMapper.toRepresentationList(checkpoints)).build();
    }

    /**
     * It connects two regions as "Neighbors".
     * URL: PUT /buildings/areas/{id1}/neighbors/{id2}
     * Example: PUT /buildings/areas/5/neighbors/6 -> Connects Area 5 with Area 6.
     */
    @PUT
    @Path("/areas/{areaId}/neighbors/{neighborId}")
    public Response connectNeighbors(@PathParam("areaId") Integer areaId,
                                     @PathParam("neighborId") Integer neighborId) {
        try {
            facilityService.connectNeighbors(areaId, neighborId);

            return Response.ok("{\"status\": \"Connected\"}").build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * He deletes a building.
     * Due to Cascade, the following will be automatically deleted:
     * 1. All Areas of the building.
     * 2. All the Checkpoints of these areas.
     */
    @DELETE
    @Path("/{id}")
    public Response deleteBuilding(@PathParam("id") Integer id) {
        boolean deleted = facilityService.deleteBuilding(id);

        if (deleted) {
            // 204 No Content
            return Response.noContent().build();
        } else {
            // 404 Not Found
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * It disconnects two regions (they are no longer neighbors).
     * URL: DELETE /buildings/areas/{id1}/neighbors/{id2}
     */
    @DELETE
    @Path("/areas/{areaId}/neighbors/{neighborId}")
    public Response disconnectNeighbors(@PathParam("areaId") Integer areaId,
                                        @PathParam("neighborId") Integer neighborId) {
        try {
            facilityService.disconnectNeighbors(areaId, neighborId);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}