package org.aueb.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.aueb.domain.User;
import org.aueb.dto.UserCreationDTO;
import org.aueb.persistence.UserRepository;

import java.net.URI;
import java.util.List;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserRepository userRepository; // Inject το Repository

    @GET
    public List<User> getAllUsers() {
        return userRepository.listAll();
    }

    @GET
    @Path("/{id}")
    public Response getUserById(@PathParam("id") Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(user).build();
    }

    @POST
    @Transactional // Απαραίτητο για εγγραφές
    public Response createUser(UserCreationDTO dto) {
        // Έλεγχος αν υπάρχει ήδη
        if (userRepository.findByUsername(dto.username) != null) {
            return Response.status(Response.Status.CONFLICT).entity("Username exists").build();
        }

        User newUser = new User(dto.username, dto.password, dto.firstName, dto.lastName, dto.email, dto.userType);

        userRepository.persist(newUser);

        return Response.created(URI.create("/users/" + newUser.getUserId())).entity(newUser).build();
    }
}