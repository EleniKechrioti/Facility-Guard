package org.aueb.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.aueb.domain.AccessCard;
import org.aueb.domain.User;
import org.aueb.representation.AccessCardMapper;
import org.aueb.persistence.AccessCardRepository;
import org.aueb.persistence.UserRepository;
import org.aueb.representation.AccessCardRepresentation;

import java.net.URI;
import java.util.Date;

@Path("/cards")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccessCardResource {

    @Inject
    AccessCardRepository cardRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    AccessCardMapper cardMapper;

    /**
     * Επιστρέφει μια κάρτα με βάση το ID της.
     */
    @GET
    @Path("/{id}")
    public Response getCardById(@PathParam("id") int cardId) {
        // Παρατήρηση: Στο Repository το findById περιμένει Long, άρα κάνουμε cast αν χρειαστεί
        // ή αλλάζουμε τον τύπο του ID στο Panache. Εδώ υποθέτουμε cast.
        AccessCard card = cardRepository.findById((long) cardId);

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
     * Αυτό είναι το πιο σημαντικό endpoint.
     */
    @POST
    @Path("/issue")
    @Transactional
    public Response issueCard(IssueCardRequest request) {
        // 1. Βρίσκουμε τον χρήστη
        User user = userRepository.findById((long) request.userId);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("User not found").build();
        }

        try {
            // 2. ΚΑΛΟΥΜΕ ΤΟ DOMAIN LOGIC!
            // Η μέθοδος issueAccessCard ελέγχει ΑΥΤΟΜΑΤΑ αν υπάρχει εγκεκριμένη αίτηση.
            // Αν δεν υπάρχει, πετάει IllegalStateException.
            AccessCard newCard = user.issueAccessCard(request.expirationDate);

            // 3. Αποθήκευση (Λόγω Cascade.ALL στον User, ίσως να μην χρειαζόταν persist,
            // αλλά το κάνουμε explicit για σιγουριά).
            cardRepository.persist(newCard);

            // 4. Επιστροφή
            return Response.created(URI.create("/cards/" + newCard.getCardId()))
                    .entity(cardMapper.toRepresentation(newCard))
                    .build();

        } catch (IllegalStateException e) {
            // Αν δεν υπάρχει approved request ή αν έχει ήδη κάρτα
            return Response.status(Response.Status.BAD_REQUEST) // ή 409 Conflict
                    .entity(e.getMessage()) // "Cannot issue card: No ACTIVE and APPROVED request..."
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
        AccessCard card = cardRepository.findById((long) cardId);
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

    // ==========================================
    // Inner DTO Class για το Input της issueCard
    // ==========================================
    public static class IssueCardRequest {
        public int userId;
        public Date expirationDate;
    }
}