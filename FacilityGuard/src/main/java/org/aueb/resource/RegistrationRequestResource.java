package org.aueb.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.aueb.domain.RegistrationRequest;
import org.aueb.domain.User;
import org.aueb.representation.RegistrationRequestMapper;
import org.aueb.persistence.RegistrationRequestRepository;
import org.aueb.persistence.UserRepository;
import org.aueb.representation.RegistrationRequestRepresentation;

import java.net.URI;
import java.util.List;

@Path("/requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegistrationRequestResource {

    @Inject
    RegistrationRequestRepository requestRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    RegistrationRequestMapper requestMapper;

    /**
     * Υποβολή νέας αίτησης εγγραφής.
     * Το JSON που στέλνει ο client πρέπει να περιέχει τουλάχιστον το ID του χρήστη.
     * Παράδειγμα body: { "user": { "id": 5 } }
     */
    @POST
    @Transactional
    public Response submitRequest(RegistrationRequestRepresentation requestRep) {
        // 1. Βρίσκουμε τον χρήστη που κάνει την αίτηση
        if (requestRep.user == null || requestRep.user.id == 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("User ID is required").build();
        }

        User user = userRepository.findById(requestRep.user.id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("User not found").build();
        }

        try {
            // 2. Χρησιμοποιούμε τη Business Logic του Domain
            // Αυτό ελέγχει αυτόματα αν υπάρχει ήδη ενεργή αίτηση
            RegistrationRequest newRequest = user.submitRegistrationRequest();

            // 3. Αποθήκευση
            requestRepository.persist(newRequest);

            // 4. Επιστροφή απάντησης (μετατροπή σε Representation)
            return Response.created(URI.create("/requests/" + newRequest.getRegistrationId()))
                    .entity(requestMapper.toRepresentation(newRequest))
                    .build();

        } catch (IllegalStateException e) {
            // Αν ο χρήστης έχει ήδη αίτηση, επιστρέφουμε 409 Conflict
            return Response.status(Response.Status.CONFLICT)
                    .entity(e.getMessage()).build();
        }
    }

    /**
     * Επιστρέφει όλες τις εκκρεμείς αιτήσεις για τον Admin.
     */
    @GET
    @Path("/pending")
    public List<RegistrationRequestRepresentation> getPendingRequests() {
        List<RegistrationRequest> pending = requestRepository.findPendingRequests();
        return requestMapper.toRepresentationList(pending);
    }

    /**
     * Έγκριση μιας αίτησης.
     * URL: PUT /requests/{id}/approve?adminId=1
     */
    @PUT
    @Path("/{id}/approve")
    @Transactional
    public Response approveRequest(@PathParam("id") int requestId, @QueryParam("adminId") int adminId) {
        return updateRequestStatus(requestId, adminId, true);
    }

    /**
     * Απόρριψη μιας αίτησης.
     * URL: PUT /requests/{id}/reject?adminId=1
     */
    @PUT
    @Path("/{id}/reject")
    @Transactional
    public Response rejectRequest(@PathParam("id") int requestId, @QueryParam("adminId") int adminId) {
        return updateRequestStatus(requestId, adminId, false);
    }

    // Βοηθητική μέθοδος για να μην γράφουμε διπλό κώδικα (DRY)
    private Response updateRequestStatus(int requestId, int adminId, boolean approve) {
        // 1. Βρίσκουμε την αίτηση
        RegistrationRequest request = requestRepository.findById((long) requestId);
        if (request == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Request not found").build();
        }

        // 2. Βρίσκουμε τον Admin (για τον έλεγχο ασφαλείας)
        if (adminId == 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("adminId query param is required").build();
        }
        User adminUser = userRepository.findById((adminId));
        if (adminUser == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Admin user not found").build();
        }

        try {
            // 3. Καλούμε τη Business Logic
            request.setApprovedStatus(approve, adminUser);

            // Το Transactional θα κάνει commit αυτόματα τις αλλαγές
            return Response.ok(requestMapper.toRepresentation(request)).build();

        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}