package org.aueb.domain;

import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class RegistrationRequestTest {

    private User adminUser;
    private User employeeUser;
    private RegistrationRequest request;

    @BeforeEach
    void setUp() {
        adminUser = new User("admin", "pass", "A", "A", "a@a.com", UserType.Administrator);
        employeeUser = new User("emp", "pass", "E", "E", "e@e.com", UserType.Employee);
        request = new RegistrationRequest(); // Ξεκινάει ACTIVE, approved=false
    }

    @Test
    void testSetApprovedStatus_SuccessApproval() {
        // Ο Admin εγκρίνει
        assertDoesNotThrow(() ->
                request.setApprovedStatus(true, adminUser));

        assertTrue(request.isApproved(), "Request should be approved.");
        assertEquals(ActivityStatus.Active, request.getStatus(),
                "Status should remain ACTIVE after approval.");
    }

    @Test
    void testSetApprovedStatus_SuccessRejection() {
        // Ο Admin απορρίπτει
        assertDoesNotThrow(() ->
                request.setApprovedStatus(false, adminUser));

        assertFalse(request.isApproved(), "Request should be rejected.");
        assertEquals(ActivityStatus.Inactive, request.getStatus(),
                "Status should become INACTIVE after rejection.");
    }

    @Test
    void testSetApprovedStatus_FailureUnauthorizedUser() {
        // Ο Employee επιχειρεί να εγκρίνει
        assertThrows(SecurityException.class, () ->
                request.setApprovedStatus(true, employeeUser));

        assertFalse(request.isApproved(), "Request should not be approved by non-administrator.");
    }

    @Test
    void testSetApprovedStatus_FailureInactiveRequest() {
        // Κάνουμε το αίτημα INACTIVE
        request.setStatus(ActivityStatus.Inactive);

        // Ο Admin επιχειρεί να το αλλάξει
        assertThrows(IllegalStateException.class, () ->
                        request.setApprovedStatus(true, adminUser),
                "Cannot change status for an already INACTIVE request.");
    }
}