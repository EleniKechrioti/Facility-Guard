package org.aueb.domain;

import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class PermissionTest {

    private User adminUser;
    private User employeeUser;
    private AccessCard testCard;
    private Permission permission;

    @BeforeEach
    void setUp() {
        // Δημιουργία χρηστών με διαφορετικούς ρόλους
        adminUser = new User("admin", "pass", "A", "A", "a@a.com", UserType.Administrator);
        employeeUser = new User("emp", "pass", "E", "E", "e@e.com", UserType.Employee);

        // Δημιουργία κάρτας (χρησιμοποιείται μόνο για σύνδεση)
        testCard = new AccessCard(new Date(System.currentTimeMillis() + 86400000)); // Λήγει αύριο

        // Δημιουργία αρχικής άδειας
        permission = new Permission(PermissionType.AccessDenied, testCard);
    }

    @Test
    void testUpdatePermissionType_SuccessByAdministrator() {
        // Ο Administrator αλλάζει την άδεια σε Granted
        assertDoesNotThrow(() ->
                        permission.updatePermissionType(PermissionType.AccessGranted, adminUser),
                "Administrator should be able to update the permission type.");

        assertEquals(PermissionType.AccessGranted, permission.getAccessGranted(),
                "PermissionType should be updated to AccessGranted.");
    }

    @Test
    void testUpdatePermissionType_FailureByEmployee() {
        // Ο Employee επιχειρεί να αλλάξει την άδεια - Πρέπει να πετάξει SecurityException
        assertThrows(SecurityException.class, () ->
                        permission.updatePermissionType(PermissionType.AccessGranted, employeeUser),
                "Non-Administrator user must throw SecurityException.");

        // Επιβεβαιώνουμε ότι η τιμή δεν άλλαξε
        assertEquals(PermissionType.AccessDenied, permission.getAccessGranted(),
                "PermissionType should remain AccessDenied after unauthorized attempt.");
    }
}
