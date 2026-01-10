package org.aueb.representation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.aueb.domain.AccessCard;
import org.aueb.domain.User;
import org.aueb.util.enumerations.ActivityStatus;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AccessCardMapperTest {

    @Inject
    AccessCardMapper accessCardMapper;

    @Test
    void testToRepresentation() {
        // 1. Προετοιμασία Entity
        AccessCard accessCard = new AccessCard();
        accessCard.setExpirationDate(new Date());
        accessCard.setStatus(ActivityStatus.Active);

        // Ορισμός ID επειδή δεν υπάρχει setter
        setPrivateField(accessCard, "cardId", 10);

        // Προετοιμασία User για τη σχέση (για να ελεγχθεί το mapping user -> holder)
        User user = new User();
        // user.setUsername("testuser");
        accessCard.setUser(user);

        // 2. Κλήση του Mapper
        AccessCardRepresentation representation = accessCardMapper.toRepresentation(accessCard);

        // 3. Έλεγχοι
        assertNotNull(representation);
        assertEquals(10, representation.id); // Ελέγχουμε αν το cardId πήγε στο id
        assertEquals(accessCard.getExpirationDate(), representation.expirationDate);
        assertEquals(accessCard.getStatus(), representation.status);

        // Έλεγχος ότι ο UserMapper κλήθηκε και μετέτρεψε τον χρήστη
        assertNotNull(representation.holder);
    }

    @Test
    void testToModel() {
        // 1. Προετοιμασία Representation
        AccessCardRepresentation representation = new AccessCardRepresentation();
        representation.id = 999; // Θα πρέπει να αγνοηθεί βάσει του ignore=true
        representation.expirationDate = new Date();
        representation.status = ActivityStatus.Inactive;

        // Προετοιμασία Holder (UserRepresentation)
        UserRepresentation holder = new UserRepresentation();
        // holder.username = "testuser";
        representation.holder = holder;

        // 2. Κλήση του Mapper
        AccessCard accessCard = accessCardMapper.toModel(representation);

        // 3. Έλεγχοι
        assertNotNull(accessCard);

        // Το cardId δεν πρέπει να είναι 999, καθώς έχουμε @Mapping(target = "cardId", ignore = true)
        assertNotEquals(999, accessCard.getCardId());

        assertEquals(representation.expirationDate, accessCard.getExpirationDate());
        assertEquals(representation.status, accessCard.getStatus());

        // Έλεγχος ότι το holder μετατράπηκε σωστά σε User entity
        assertNotNull(accessCard.getUser());
    }

    /**
     * Βοηθητική μέθοδος για να ορίσουμε τιμή σε private πεδίο (όπως το cardId)
     * που δεν έχει public setter.
     */
    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}