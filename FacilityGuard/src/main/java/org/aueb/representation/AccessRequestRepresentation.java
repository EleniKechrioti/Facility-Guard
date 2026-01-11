package org.aueb.representation;

import org.aueb.util.enumerations.AccessType;

/**
 * DTO representing the JSON payload sent by a Card Reader device.
 * Example JSON:
 * {
 * "cardId": 123,
 * "checkpointId": 5,
 * "accessType": "In"
 * }
 */
public class AccessRequestRepresentation {
    public Integer cardId;
    public Integer checkpointId;
    public AccessType accessType; // Enum: In / Out
}