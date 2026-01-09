package org.aueb.representation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public class AreaRepresentation {
    public Integer id;

    @NotBlank(message = "Το όνομα της ζώνης είναι υποχρεωτικό")
    @Size(max = 100, message = "Το όνομα δεν μπορεί να ξεπερνά τους 100 χαρακτήρες")
    public String name;

    // We return the building ID for reference
    public Integer buildingId;

    // List of IDs of neighboring zones (so we know where you can go)
    public Set<Integer> neighborIds;

    // List of checkpoint names (for simple viewing)
    public Set<String> checkpointNames;
}