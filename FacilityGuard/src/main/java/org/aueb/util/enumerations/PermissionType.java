package org.aueb.util.enumerations;

/**
 * Specifies whether the permissions given have access rights or not.
 */
public enum PermissionType {
    /**
     * The access card has the right to enter an area, and permission is granted.
     */
    AccessGranted,

    /**
     * The access card has not the right to enter an area, and permission is denied.
     */
    AccessDenied
}