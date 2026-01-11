package org.aueb.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.aueb.domain.Area;
import org.aueb.representation.AccessRequestRepresentation;
import org.aueb.service.AccessControlService;

@Path("/access")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccessResource {

    @Inject
    AccessControlService accessService;

    /**
     * Endpoint for Access Requests (e.g., from a physical Card Reader).
     * URL: POST /access/request
     */
    @POST
    @Path("/request")
    public Response requestAccess(AccessRequestRepresentation request) {
        // Ensure all necessary fields are present
        if (request == null || request.cardId == null || request.checkpointId == null || request.accessType == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Missing parameters (cardId, checkpointId, accessType)\"}")
                    .build();
        }

        // Delegate logic to the Service Layer
        boolean granted = accessService.requestAccess(request.cardId, request.checkpointId, request.accessType);

        // Return appropriate HTTP Status
        if (granted) {
            // HTTP 200 OK: Access Granted (Open the door)
            return Response.ok("{\"status\": \"GRANTED\"}").build();
        } else {
            // HTTP 403 Forbidden: Access Denied (Flash red light)
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"status\": \"DENIED\"}")
                    .build();
        }
    }

    /**
     * Monitoring Endpoint: Check current location of a card holder.
     * URL: GET /access/location/{cardId}
     */
    @GET
    @Path("/location/{cardId}")
    public Response getLocation(@PathParam("cardId") Integer cardId) {
        Area area = accessService.findCurrentLocation(cardId);

        if (area != null) {
            // User is INSIDE a specific area
            return Response.ok("{\"status\": \"INSIDE\", \"areaId\": " + area.getAreaId() + ", \"areaName\": \"" + area.getName() + "\"}")
                    .build();
        } else {
            // User is OUTSIDE (or no history)
            return Response.ok("{\"status\": \"OUTSIDE\"}").build();
        }
    }
}