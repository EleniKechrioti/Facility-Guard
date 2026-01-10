package org.aueb.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.aueb.domain.AccessCard;
import org.aueb.domain.Area;
import org.aueb.domain.Permission;
import org.aueb.domain.User;
import org.aueb.representation.AccessCardMapper;
import org.aueb.representation.PermissionMapper;
import org.aueb.persistence.AccessCardRepository;
import org.aueb.persistence.AreaRepository;
import org.aueb.persistence.PermissionRepository;
import org.aueb.persistence.UserRepository;
import org.aueb.representation.PermissionRepresentation;
import org.aueb.service.AccessControlService;
import org.aueb.util.enumerations.PermissionType;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Path("/cards")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccessCardResource {

    @Inject
    AccessCardRepository cardRepository;

    @Inject
    UserRepository userRepository;

    // Προσθήκη Repositories για Permissions/Areas
    @Inject
    PermissionRepository permissionRepository;

    @Inject
    AreaRepository areaRepository;

    @Inject
    AccessCardMapper cardMapper;

    @Inject
    PermissionMapper permissionMapper;

    @Inject
    AccessControlService accessService;

    /**
     * Επιστρέφει μια κάρτα με βάση το ID της.
     */
    @GET
    @Path("/{id}")
    public Response getCardById(@PathParam("id") int cardId) {
        AccessCard card = cardRepository.findById(cardId);

        if (card == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(cardMapper.toRepresentation(card)).build();
    }

    /**
     * Βρίσκει την κάρτα ενός συγκεκριμένου χρήστη.
     * URL: GET /cards/user/5
     */
    @GET
    @Path("/user/{userId}")
    public Response getCardByUserId(@PathParam("userId") int userId) {
        AccessCard card = cardRepository.findByUserId(userId);

        if (card == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("User has no active card").build();
        }

        return Response.ok(cardMapper.toRepresentation(card)).build();
    }

    /**
     * Έκδοση νέας κάρτας.
     */
    @POST
    @Path("/issue")
    @Transactional
    public Response issueCard(IssueCardRequest request) {
        // 1. Βρίσκουμε τον χρήστη
        User user = userRepository.findById(request.userId);

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("User not found").build();
        }

        try {
            // 2. Domain Logic
            AccessCard newCard = user.issueAccessCard(request.expirationDate);

            // 3. Persist
            cardRepository.persist(newCard);

            // 4. Επιστροφή
            return Response.created(URI.create("/cards/" + newCard.getCardId()))
                    .entity(cardMapper.toRepresentation(newCard))
                    .build();

        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

    /**
     * Ακύρωση κάρτας.
     */
    @PUT
    @Path("/{id}/deactivate")
    @Transactional
    public Response deactivateCard(@PathParam("id") int cardId) {
        AccessCard card = cardRepository.findById(cardId);

        if (card == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            card.deactivateCard(); // Domain Logic
            return Response.ok(cardMapper.toRepresentation(card)).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).build();
        }
    }

    // SUB-RESOURCE: PERMISSIONS

    /**
     * Λήψη όλων των δικαιωμάτων μιας κάρτας.
     * URL: GET /cards/{id}/permissions
     */
    @GET
    @Path("/{id}/permissions")
    public Response getCardPermissions(@PathParam("id") int cardId) {
        // Ελέγχουμε αν υπάρχει η κάρτα
        if (cardRepository.findById(cardId) == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Χρήση του PermissionRepository που φτιάξαμε
        List<PermissionRepresentation> permissions = permissionRepository.findByCardId(cardId)
                .stream()
                .map(permissionMapper::toRepresentation)
                .collect(Collectors.toList());

        return Response.ok(permissions).build();
    }

    /**
     * Ανάθεση δικαιώματος πρόσβασης σε περιοχή.
     * URL: POST /cards/{id}/permissions
     * Body: { "areaId": 5, "type": "AccessGranted" }
     */
    @POST
    @Path("/{id}/permissions")
    @Transactional
    public Response grantPermission(@PathParam("id") int cardId, GrantAccessRequest request) {
        // 1. Έλεγχος Κάρτας
        AccessCard card = cardRepository.findById(cardId);
        if (card == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Card not found").build();
        }

        // 2. Έλεγχος Περιοχής
        Area area = areaRepository.findById(request.areaId);
        if (area == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Area not found").build();
        }

        // 3. Έλεγχος αν υπάρχει ήδη δικαίωμα (για να μην έχουμε διπλότυπα)
        if (permissionRepository.findByCardAndArea(cardId, request.areaId).isPresent()) {
            return Response.status(Response.Status.CONFLICT).entity("Permission already exists for this area").build();
        }

        // 4. Δημιουργία Permission
        PermissionType type = (request.type != null) ? request.type : PermissionType.AccessGranted;
        Permission newPermission = new Permission(type, card, area);

        permissionRepository.persist(newPermission);

        return Response.status(Response.Status.CREATED)
                .entity(permissionMapper.toRepresentation(newPermission))
                .build();
    }

    @GET
    @Path("/location/{cardId}")
    public Response getLocation(@PathParam("cardId") Integer cardId) {
        Area area = accessService.findCurrentLocation(cardId);

        if (area != null) {
            return Response.ok("{\"status\": \"INSIDE\", \"area\": \"" + area.getName() + "\"}").build();
        } else {
            return Response.ok("{\"status\": \"OUTSIDE\"}").build();
        }
    }

    // Inner DTO Classes

    public static class IssueCardRequest {
        public int userId;
        public Date expirationDate;
    }

    public static class GrantAccessRequest {
        public int areaId;
        public PermissionType type; // default AccessGranted
    }
}