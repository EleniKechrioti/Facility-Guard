package org.aueb.util.enumerations;

/**
 * Defines the direction of an access event recorded in the AccessLog.
 * Typically used to differentiate between entry and exit movements.
 */
public enum AccessType {
    /**
     * Indicates an entry movement or access INTO a protected area/building.
     */
    In,

    /**
     * Indicates an exit movement or access OUT of a protected area/building.
     */
    Out
}
