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
        request = new RegistrationRequest();
    }

    @Test
    void testSetApprovedStatus_SuccessApproval() {
        /** Admin approves  */
        assertDoesNotThrow(() ->
                request.setApprovedStatus(true, adminUser));

        assertTrue(request.isApproved(), "Request should be approved.");
        assertEquals(ActivityStatus.Active, request.getStatus(),
                "Status should remain ACTIVE after approval.");
    }

    @Test
    void testSetApprovedStatus_SuccessRejection() {
        /** Admin rejects   */
        assertDoesNotThrow(() ->
                request.setApprovedStatus(false, adminUser));

        assertFalse(request.isApproved(), "Request should be rejected.");
        assertEquals(ActivityStatus.Inactive, request.getStatus(),
                "Status should become INACTIVE after rejection.");
    }

    @Test
    void testSetApprovedStatus_FailureUnauthorizedUser() {
        /** Employee tries to approve   */
        assertThrows(SecurityException.class, () ->
                request.setApprovedStatus(true, employeeUser));

        assertFalse(request.isApproved(), "Request should not be approved by non-administrator.");
    }

    @Test
    void testSetApprovedStatus_FailureInactiveRequest() {
        /** Sets the status to Inactive   */
        request.setStatus(ActivityStatus.Inactive);

        /** Admin tries to change it   */
        assertThrows(IllegalStateException.class, () ->
                        request.setApprovedStatus(true, adminUser),
                "Cannot change status for an already INACTIVE request.");
    }

    @Test
    void testInvalidateRequest_SuccessAndSecurityFailure() {
        assertDoesNotThrow(() ->
                request.invalidateRequest(adminUser));

        assertEquals(ActivityStatus.Inactive, request.getStatus(),
                "Status must be INACTIVE after invalidation.");
        assertFalse(request.isApproved(), "Approved flag must be set to false after invalidation.");

        request = new RegistrationRequest();

        assertThrows(SecurityException.class, () ->
                        request.invalidateRequest(employeeUser),
                "Only an Administrator should be able to invalidate a request.");
    }

    @Test
    void testConstructor_InitialState() {
        assertFalse(request.isApproved(), "New request should start as NOT approved (false).");
        assertEquals(ActivityStatus.Active, request.getStatus(),
                "New request should start with ACTIVE status.");
        assertNotNull(request.getRequestDate(), "Request date must be initialized.");
    }

    @Test
    void testSetUser_BidirectionalSafetyCheck() {
        User user = new User("test_user", "p", "T", "T", "t@t.com", UserType.Employee);

        request.setUser(user);

        assertSame(user, request.getUser(), "User link must be set.");
        assertTrue(user.getRegistrationRequests().contains(request),
                "Request must be added to the user's collection.");

        request.setUser(user);

        assertEquals(1, user.getRegistrationRequests().size(), "Set size must not change on second call.");
    }
}