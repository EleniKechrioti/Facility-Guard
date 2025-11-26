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
    private Area area;

    @BeforeEach
    void setUp() {
        /** Creation of users with different roles   */
        adminUser = new User("admin", "pass", "A", "A", "a@a.com", UserType.Administrator);
        employeeUser = new User("emp", "pass", "E", "E", "e@e.com", UserType.Employee);

        /** Creation of card (used only for login)  */
        testCard = new AccessCard(new Date(System.currentTimeMillis() + 86400000)); // Λήγει αύριο
        area = new Area("Test Area", new Building("Test Building", null));
        /**  Creation of new Permission   */
        permission = new Permission(PermissionType.AccessDenied, testCard, area);
    }

    @Test
    void testUpdatePermissionType_SuccessByAdministrator() {
        /** The Administrator changes the Permission to Granted   */
        assertDoesNotThrow(() ->
                        permission.updatePermissionType(PermissionType.AccessGranted, adminUser),
                "Administrator should be able to update the permission type.");

        assertEquals(PermissionType.AccessGranted, permission.getAccessGranted(),
                "PermissionType should be updated to AccessGranted.");
    }

    @Test
    void testUpdatePermissionType_FailureByEmployee() {
        /**  Employee tries to change the persmission - Should throw a SecurityException   */
        assertThrows(SecurityException.class, () ->
                        permission.updatePermissionType(PermissionType.AccessGranted, employeeUser),
                "Non-Administrator user must throw SecurityException.");

        /** We confirm that the value did not change  */
        assertEquals(PermissionType.AccessDenied, permission.getAccessGranted(),
                "PermissionType should remain AccessDenied after unauthorized attempt.");
    }
}
