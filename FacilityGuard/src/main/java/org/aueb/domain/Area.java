package org.aueb.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a secured area or zone within a Building.
 * Access to an Area is controlled by one or more Checkpoints.
 * This entity implements Serializable as required for JPA entities in enterprise environments.
 */
@Entity
@Table(name = "areas")
public class Area implements Serializable {

    /** The unique identifier for the area.*/
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int areaId;

    /** The common name or description of the area (e.g., "Server Room", "Ground Floor").*/
    @Column(nullable = false, length = 100)
    private String name;


    /**
     * The Building to which this Area belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    /**
     * The set of Checkpoints that control access to this Area.
     * Mapped by the 'area' field in the Checkpoint entity.
     * orphanRemoval=true ensures that if a Checkpoint is removed from this set, it is deleted from the database.
     */
    @OneToMany(mappedBy = "area", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Checkpoint> checkpoints = new HashSet<>();

    /**
     * The set of neighboring Areas (Many-to-Many self-reference).
     * This establishes a symmetrical relationship.
     * * @JoinTable defines the intermediary table (area_neighbors).
     * - joinColumns: Refers to the foreign key pointing to THIS Area (areaId).
     * - inverseJoinColumns: Refers to the foreign key pointing to the OTHER Area (the neighbor).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "area_neighbors",
            joinColumns = @JoinColumn(name = "area_id"),
            inverseJoinColumns = @JoinColumn(name = "neighbor_id")
    )
    private Set<Area> neighbors = new HashSet<>();

    /** Default constructor.*/
    public Area() {
    }

    /**
     * Constructor for creating a new Area object.
     * @param name The name of the area.
     * @param building The parent Building entity.
     */
    public Area(String name, Building building) {
        this.name = name;
        this.building = building;
    }

    // --- Getters and Setters ---

    /**
     * Returns the unique ID of the area.
     * @return the area ID.
     */
    public int getAreaId() { return areaId; }

    /**
     * Sets the unique ID of the area.
     * @param areaId the area ID.
     */
    public void setAreaId(int areaId) { this.areaId = areaId; }

    /**
     * Returns the name of the area.
     * @return the area name.
     */
    public String getName() { return name; }

    /**
     * Sets the name of the area.
     * @param name the area name.
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the parent Building entity.
     * @return the associated Building.
     */
    public Building getBuilding() { return building; }

    /**
     * Sets the parent Building entity.
     * @param building the parent Building to set.
     */
    public void setBuilding(Building building) { this.building = building; }

    /**
     * Returns the set of Checkpoints associated with this Area.
     * @return the collection of Checkpoint entities.
     */
    public Set<Checkpoint> getCheckpoints() { return checkpoints; }

    /**
     * Sets the set of Checkpoints.
     * @param checkpoints the set of Checkpoints.
     */
    public void setCheckpoints(Set<Checkpoint> checkpoints) { this.checkpoints = checkpoints; }


    /**
     * Adds a Checkpoint to the collection.
     * @param checkpoint The Checkpoint to add.
     */
    public void addCheckpoint(Checkpoint checkpoint) {
        checkpoints.add(checkpoint);
        checkpoint.setArea(this);
    }

    /**
     * Removes a Checkpoint from the collection.
     * @param checkpoint The Checkpoint to remove.
     */
    public void removeCheckpoint(Checkpoint checkpoint) {
        checkpoints.remove(checkpoint);
        checkpoint.setArea(null);
    }

    /**
     * Returns the set of neighboring Areas.
     * @return the set of neighbors.
     */
    public Set<Area> getNeighbors() {
        return neighbors;
    }

    /**
     * Sets the set of neighbors.
     * @param neighbors the set of neighbors.
     */
    public void setNeighbors(Set<Area> neighbors) {
        this.neighbors = neighbors;
    }

    /**
     * Adds a neighbor to the set.
     * @param neighbor The Area to add as a neighbor.
     */
    public void addNeighbor(Area neighbor) {
        this.neighbors.add(neighbor);
        neighbor.getNeighbors().add(this);
    }

    /**
     * Removes the symmetrical neighbor relationship.
     * @param neighbor The Area to remove.
     */
    public void removeNeighbor(Area neighbor) {
        // Remove the neighbor from this area's list
        this.neighbors.remove(neighbor);

        // remove this area from the neighbor's list
        neighbor.getNeighbors().remove(this);
    }

    /**
     * Checks if this {@link Area} object is a neighbor of another Area object.
     * @param otherArea The area to be compared.
     * @return true if the otherArea is contained in the neighbor's set.
     */
    public boolean isNeighborOf(Area otherArea) {
        if (otherArea == null) {
            return false;
        }
        return this.neighbors.contains(otherArea);
    }

    /**
     * Searches for a checkpoint within the Area object by its name.
     * @param name the name of the checkpoint
     * @return the checkpoint if its found, else null.
     */
    public Checkpoint getCheckpointByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return this.checkpoints.stream()
                .filter(cp -> cp.getName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Implements equality based on the primary key (areaId).
     * @param o the other object.
     * @return {@code true} if the objects are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Area area = (Area) o;

        if (areaId != 0 && area.areaId != 0) {
            return areaId == area.areaId;
        }

        return Objects.equals(name, area.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(areaId);
    }

    /**
     * Returns a string representation of the Area object, useful for logging and debugging.
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return "Area{" +
                "areaId=" + areaId +
                ", name='" + name + '\'' +
                '}';
    }
}