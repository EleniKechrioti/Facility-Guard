package org.aueb.representation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.aueb.domain.Area;
import org.aueb.domain.Permission;
import org.aueb.util.enumerations.PermissionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

@QuarkusTest
public class PermissionMapperTest {

    @Inject
    PermissionMapper mapper;

    @Test
    public void toRepresentationTest() {
        // 1. Προετοιμασία των Entities (Area & Permission)
        Area area = new Area();
        area.setName("Server Room");
        setAreaIdViaReflection(area, 10); // Ορισμός ID για το Area

        Permission permission = new Permission();
        permission.setArea(area); // Σύνδεση Permission με Area
        // permission.setAccessGranted(PermissionType.AccessGranted); // Δεν υπάρχει setter, το κάνουμε via reflection
        // καθώς στον κώδικα Permission δεν υπάρχει απλός setter για το accessGranted,
        // υπάρχει μόνο η updatePermissionType (που θέλει admin).
        setAccessGrantedViaReflection(permission, PermissionType.AccessGranted);

        setPermissionIdViaReflection(permission, 50); // Ορισμός ID για το Permission

        // 2. Κλήση του Mapper
        PermissionRepresentation representation = mapper.toRepresentation(permission);

        // 3. Επαλήθευση (Assertions)
        Assertions.assertNotNull(representation);
        Assertions.assertEquals(50, representation.permissionId);
        Assertions.assertEquals(PermissionType.AccessGranted, representation.accessGranted);

        // Έλεγχος αν δούλεψε ο AreaMapper (Nested Mapping)
        Assertions.assertNotNull(representation.area);
        Assertions.assertEquals(10, representation.area.id);
        Assertions.assertEquals("Server Room", representation.area.name);
    }

    @Test
    public void toModelTest() {
        // 1. Προετοιμασία του Representation (DTO)
        PermissionRepresentation representation = new PermissionRepresentation();
        representation.permissionId = 100; // Θα πρέπει να περάσει στο Entity
        representation.accessGranted = PermissionType.AccessDenied;

        // Δημιουργία του Nested AreaRepresentation
        AreaRepresentation areaRep = new AreaRepresentation();
        areaRep.id = 20;
        areaRep.name = "Reception";
        representation.area = areaRep;

        // 2. Κλήση του Mapper
        Permission permission = mapper.toModel(representation);

        // 3. Επαλήθευση
        Assertions.assertNotNull(permission);

        // Έλεγχος ID
        Assertions.assertNull(permission.getAccessGranted());

        // Έλεγχος ότι η AccessCard αγνοήθηκε (βάσει του @Mapping ignore = true)
        Assertions.assertNull(permission.getAccessCard());

        // Έλεγχος αν μετατράπηκε το AreaRepresentation σε Area Entity
        Assertions.assertNotNull(permission.getArea());
        // Επειδή το Area έχει private ID χωρίς setter, το MapStruct μέσω AreaMapper
        // θα δημιουργήσει ένα Area object με τα πεδία που μπορεί να γράψει (π.χ. name).
        Assertions.assertEquals("Reception", permission.getArea().getName());
    }

    // --- Helper Methods (Reflection) ---
    // Χρησιμοποιούμε reflection γιατί τα Entities δεν έχουν Setters για τα ID

    private void setPermissionIdViaReflection(Permission permission, int id) {
        try {
            Field field = Permission.class.getDeclaredField("permissionId");
            field.setAccessible(true);
            field.setInt(permission, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set permissionId", e);
        }
    }

    private void setAccessGrantedViaReflection(Permission permission, PermissionType type) {
        try {
            Field field = Permission.class.getDeclaredField("accessGranted");
            field.setAccessible(true);
            field.set(permission, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set accessGranted", e);
        }
    }

    private void setAreaIdViaReflection(Area area, int id) {
        try {
            Field field = Area.class.getDeclaredField("areaId");
            field.setAccessible(true);
            field.setInt(area, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set areaId", e);
        }
    }
}