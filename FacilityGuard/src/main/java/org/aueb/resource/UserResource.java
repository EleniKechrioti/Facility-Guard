package org.aueb.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.aueb.domain.User;
import org.aueb.representation.UserMapper;
import org.aueb.persistence.UserRepository;
import org.aueb.representation.UserRepresentation;
import org.aueb.util.enumerations.UserType;

import java.net.URI;
import java.util.List;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserRepository userRepository;

    @Inject
    UserMapper userMapper;

    /**
     * Επιστροφή όλων των χρηστών.
     * Χρήσιμο για διαχειριστές.
     */
    @GET
    public List<UserRepresentation> getAll() {
        return userMapper.toRepresentationList(userRepository.listAll());
    }

    /**
     * Αναζήτηση χρήστη με βάση το ID.
     */
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") int id) {
        User user = userRepository.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(userMapper.toRepresentation(user)).build();
    }

    /**
     * Δημιουργία νέου χρήστη.
     * Χρησιμοποιούμε ειδικό DTO (UserCreationRequest) για να δεχτούμε και το password.
     */
    @POST
    @Transactional
    public Response create(UserCreationRequest request) {
        // Έλεγχος αν υπάρχει ήδη το username
        if (userRepository.findByUsername(request.username) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Username already exists").build();
        }

        // Δημιουργία του User entity
        // Χρησιμοποιούμε τον constructor του User για να ορίσουμε τα βασικά πεδία
        User newUser = new User(
                request.username,
                request.password, // Εδώ περνάμε το password
                request.firstName,
                request.lastName,
                request.email,
                request.userType
        );

        userRepository.persist(newUser);

        // Επιστρέφουμε 201 Created και το Representation (χωρίς password)
        return Response.created(URI.create("/users/" + newUser.getUserId()))
                .entity(userMapper.toRepresentation(newUser))
                .build();
    }

    /**
     * Ενημέρωση στοιχείων χρήστη.
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") int id, UserRepresentation dto) {
        User user = userRepository.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Ενημέρωση πεδίων (δεν πειράζουμε password ή id εδώ)
        user.setFirstName(dto.firstName);
        user.setLastName(dto.lastName);
        user.setEmail(dto.email);
        user.setUserType(dto.userType);
        // Το username συνήθως δεν αλλάζει, αλλά αν θέλουμε το βάζουμε:
        // user.setUsername(dto.username);

        // Το persist καλείται αυτόματα στο τέλος του transaction για managed objects.

        return Response.ok(userMapper.toRepresentation(user)).build();
    }

    /**
     * Διαγραφή χρήστη.
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") int id) {
        boolean deleted = userRepository.deleteById(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    // ==========================================
    // Inner DTO Class για το Input της create (POST)
    // ==========================================
    public static class UserCreationRequest {
        public String username;
        public String password; // Αυτό έλειπε από το UserRepresentation
        public String firstName;
        public String lastName;
        public String email;
        public UserType userType;
    }
}