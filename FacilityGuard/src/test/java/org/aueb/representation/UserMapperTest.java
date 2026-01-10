package org.aueb.representation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.aueb.domain.User;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

@QuarkusTest
public class UserMapperTest {

    @Inject
    UserMapper mapper;

    @Test
    public void toRepresentationTest() {
        // 1. Δημιουργία του User Entity
        User user = new User(
                "john_doe",
                "secret123",
                "John",
                "Doe",
                "john@aueb.gr",
                UserType.Visitor
        );

        // Ορισμός του userId μέσω Reflection
        setUserIdViaReflection(user, 101);

        // 2. Κλήση του Mapper
        UserRepresentation representation = mapper.toRepresentation(user);

        // 3. Επαλήθευση (Assertions)
        Assertions.assertNotNull(representation);
        Assertions.assertEquals(101, representation.id);
        Assertions.assertEquals("john_doe", representation.username);
        Assertions.assertEquals("John", representation.firstName);
        Assertions.assertEquals("Doe", representation.lastName);
        Assertions.assertEquals("john@aueb.gr", representation.email);
        Assertions.assertEquals(UserType.Visitor, representation.userType);
    }

    @Test
    public void toModelTest() {
        // 1. Δημιουργία του Representation (DTO)
        UserRepresentation representation = new UserRepresentation();
        representation.id = 999; // Θα αγνοηθεί
        representation.username = "jane_doe";
        representation.firstName = "Jane";
        representation.lastName = "Doe";
        representation.email = "jane@test.com";
        representation.userType = UserType.Administrator;

        // 2. Κλήση του Mapper
        User user = mapper.toModel(representation);

        // 3. Επαλήθευση
        Assertions.assertNotNull(user);

        // Το userId πρέπει να είναι 0 (ignore = true)
        Assertions.assertEquals(0, user.getUserId());

        Assertions.assertEquals("jane_doe", user.getUsername());
        Assertions.assertEquals("Jane", user.getFirstName());
        Assertions.assertEquals("Doe", user.getLastName());
        Assertions.assertEquals("jane@test.com", user.getEmail());
        Assertions.assertEquals(UserType.Administrator, user.getUserType());

        // Password πρέπει να είναι null
        Assertions.assertNull(user.getPassword());

        // Σχέσεις πρέπει να είναι null/empty
        Assertions.assertNull(user.getAccessCard());
        Assertions.assertTrue(user.getRegistrationRequests().isEmpty());
    }

    @Test
    public void toRepresentationListTest() {
        User u1 = new User("u1", "p", "f", "l", "e", UserType.Visitor);
        User u2 = new User("u2", "p", "f", "l", "e", UserType.Employee);

        List<UserRepresentation> reps = mapper.toRepresentationList(List.of(u1, u2));

        Assertions.assertEquals(2, reps.size());
        Assertions.assertEquals("u1", reps.get(0).username);
        Assertions.assertEquals(UserType.Employee, reps.get(1).userType);
    }

    // --- Helper Method ---
    private void setUserIdViaReflection(User user, int id) {
        try {
            Field field = User.class.getDeclaredField("userId");
            field.setAccessible(true);
            field.setInt(user, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set userId via reflection", e);
        }
    }
}