package org.aueb.representation;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.aueb.util.enumerations.PermissionType;

@RegisterForReflection
public class PermissionRepresentation {
    public Integer permissionId;
    public PermissionType accessGranted;

    // Το JSON θα περιέχει: "area": { "id": 1, "name": "Server Room", ... }
    public AreaRepresentation area;
}