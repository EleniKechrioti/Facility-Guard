package org.aueb.util.enumerations;

/**
 * Defines the different types of users allowed in the system.
 * This can be used to control role-based access to application features.
 */
public enum UserType {
    /**
     * An employee who primarily needs access to buildings/areas.
     */
    Employee,

    /**
     * An administrative user with elevated privileges to manage the system,
     * users, access cards, and permissions.
     */
    Administrator,

    /**
     * A visitor or temporary user type.
     */
    Visitor
}
