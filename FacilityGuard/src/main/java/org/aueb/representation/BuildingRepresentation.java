package org.aueb.representation;

import jakarta.validation.constraints.NotBlank;
import org.aueb.util.Address;
import java.util.List;

public class BuildingRepresentation {
    public Integer id;
    @NotBlank(message = "Το όνομα δεν μπορεί να είναι κενό")
    public String name;
    public Address address; // Address is Embeddable

    // Optionally, if we want to see the zones when requesting a building
    public List<AreaRepresentation> areas;
}