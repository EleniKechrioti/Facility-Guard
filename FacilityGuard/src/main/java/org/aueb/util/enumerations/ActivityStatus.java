package org.aueb.util.enumerations;

/**
 * Represents the current activity status of an entity, such as an AccessCard.
 * This status determines whether the entity is currently usable or operational.
 */
public enum ActivityStatus {
    /**
     * The entity is currently active and fully operational.
     */
    Active,

    /**
     * The entity is inactive and cannot be used (e.g., a blocked or expired card).
     */
    Inactive
}