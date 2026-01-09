package org.aueb.representation;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;

import java.util.Date;

@RegisterForReflection
public class AccessLogRepresentation {

    public Integer logId;
    public Date timestamp;
    public PermissionType accessGranted;
    public AccessType accessType;

    /** References only (no entities) */
    public Integer cardId;
    public Integer checkpointId;
}
