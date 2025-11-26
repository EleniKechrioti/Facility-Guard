package org.aueb.domain;

import jakarta.persistence.*;
import org.aueb.util.Address;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a physical building that contains multiple access-controlled Areas.
 * Each Building is associated with exactly one Address.
 */
@Entity
@Table(name = "buildings")
public class Building implements Serializable {


    /**
     * The unique identifier for the building.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int buildingId;

    /**
     * The name of the building.
     */
    @Column(nullable = false, length = 100)
    private String name;


    /**
     * The address of the Building.
     */
    @Embedded
    private Address address;

    /**
     * The set of Areas contained within this Building.
     */
    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Area> areas = new HashSet<>();


    /**
     * Default constructor.
     */
    public Building() {
    }

    /**
     * Constructor for creating a new Building object.
     */
    public Building(String name, Address address) {
        setName(name);
        setAddress(address);
    }

    // --- Getters and Setters ---

    /**
     * Returns the Buildings id.
     */
    public int getBuildingId() { return buildingId; }

    /**
     * Sets the building's id
     */
    public void setBuildingId(int buildingId) { this.buildingId = buildingId; }

    /**
     * Returns the name of the building.
     */
    public String getName() { return name; }

    /**
     * Sets the name of the building
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the address of the building
     */
    public Address getAddress() { return address; }

    /**
     * Sets the address of the building
     */
    public void setAddress(Address address) { this.address = address; }

    /**
     * Returns the area set of the building
     */
    public Set<Area> getAreas() { return areas; }

    /**
     * Sets the area set of the building
     */
    public void setAreas(Set<Area> areas) { this.areas = areas; }

    /**
     * Adds an Area to the collection
     */
    public void addArea(Area area) {
        areas.add(area);
        area.setBuilding(this);
    }

    /**
     * Removes an Area from the collection.
     */
    public void removeArea(Area area) {
        areas.remove(area);
        area.setBuilding(null);
    }

    /**
     * Searches for an area within a Building with its name.
     * @return the area if its found, else null.
     */
    public Area getAreaByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return this.areas.stream()
                .filter(area -> area.getName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if the building contains an area with a specific ID.
     * @return true if its found.
     */
    public boolean containsArea(int areaId) {
        if (areaId <= 0) {
            return false;
        }
        return this.areas.stream()
                .anyMatch(area -> area.getAreaId() == areaId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Building building = (Building) o;
        return buildingId == building.buildingId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(buildingId);
    }
}