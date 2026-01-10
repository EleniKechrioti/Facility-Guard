package org.aueb.representation;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CheckpointRepresentation {

    public Integer checkpointId;
    public String name;

    public Integer areaId;
    public Integer permissionId;
}
