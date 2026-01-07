package org.aueb.domain;

import org.aueb.util.Address;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Building domain entity.
 * This class verifies the correct functionality of constructors, attribute assignments,
 * and core domain logic related to location management and relationships, independent of the
 * persistence layer.
 */
public class BuildingTest {

    /**
     * Checks if an entity building is created correctly.
     */
    @Test
    void createBuildingAndCheckAddress() {
        Address address = new Address("Test St", "1", "Test City", "10000", "GR");
        String name = "Main Campus Building";

        Building building = new Building(name, address);

        assertNotNull(building, "Building object should not be null.");
        assertEquals(name, building.getName(), "Building name should match the constructor argument.");
        assertEquals(address.getCity(), building.getAddress().getCity(), "Address should be correctly embedded.");
    }

    /**
     * Checks if areas are registered correctly
     */
    @Test
    void addAreaAndCheckBidirectionalLink() {

        Address address = new Address("Test St", "1", "Test City", "10000", "GR");
        Building building = new Building("Main Campus", address);
        Area area1 = new Area("Server Room", building);
        Area area2 = new Area("Lab 205", building);

        building.addArea(area1);
        building.addArea(area2);

        assertEquals(2, building.getAreas().size(), "Building must contain two areas.");

        assertTrue(building.getAreas().contains(area1), "Area 1 must be in the Building's set.");
        assertEquals(building, area1.getBuilding(), "Area 1 must reference the correct Building (back-reference).");
        assertEquals(building, area2.getBuilding(), "Area 2 must reference the correct Building (back-reference).");
    }

    /**
     * Checks if an area can be found successfully within a building, if it exists in it.
     */
    @Test
    void getAreaByName() {
        Building building = new Building("Main Campus", null);
        Area officeArea = new Area("Central Office", building);
        Area serverArea = new Area("Server Room", building);

        building.addArea(officeArea);
        building.addArea(serverArea);

        Area foundArea = building.getAreaByName("Central Office");
        Area notFoundArea = building.getAreaByName("Unknown Zone");

        assertNotNull(foundArea, "Should find the Area with the exact name.");
        assertEquals(officeArea, foundArea, "The retrieved object should be the correct Area instance.");
        assertNull(notFoundArea, "Should return null if the Area name does not exist.");
    }

    /**
     * Checks if an area can be found with its id within a building, if it exists.
     */
    @Test
    void containsArea_CheckById() {
        Building building = new Building("ID Check", null);
        Area areaWithId = new Area("ID Area", building);
        areaWithId.setAreaId(99);

        building.addArea(areaWithId);

        assertTrue(building.containsArea(99), "Should return true for a contained Area's ID.");
        assertFalse(building.containsArea(100), "Should return false for a non-existent ID.");
        assertFalse(building.containsArea(0), "Should return false for an invalid ID (0).");
    }

    /**
     * Checks if an Area is correctly removed from the collection and if the bidirectional
     */
    @Test
    void testRemoveAreaAndCheckBidirectionalLink() {
        Address address = new Address("Test St", "1", "Test City", "10000", "GR");
        Building building = new Building("Main Campus", address);
        Area areaToRemove = new Area("Warehouse", building);
        Area areaToKeep = new Area("Office", building);

        building.addArea(areaToRemove);
        building.addArea(areaToKeep);

        assertEquals(2, building.getAreas().size());
        assertEquals(building, areaToRemove.getBuilding());

        building.removeArea(areaToRemove);

        assertEquals(1, building.getAreas().size(), "Building must contain only one area after removal.");
        assertFalse(building.getAreas().contains(areaToRemove), "The removed Area should not be in the collection.");

        assertNull(areaToRemove.getBuilding(), "The back-reference in the removed Area must be set to null.");
    }

    /**
     * Checks equals() and hashCode() consistency
     */
    @Test
    void testBuildingEqualityAndHashCode() {
        Address address = new Address("A", "1", "C", "1", "G");
        Building b1 = new Building("Main", address); // ID = 0
        Building b2 = new Building("Main", address); // ID = 0
        Building b3 = new Building("Aux", address);

        b1.setBuildingId(10);
        b3.setBuildingId(10);

        assertEquals(b1, b3, "Buildings with the same ID must be equal (Persistence Identity).");
        assertNotEquals(b1, b2, "Buildings with different IDs/references must not be equal.");

        assertEquals(b1.hashCode(), b3.hashCode(), "Buildings with same ID must have the same hash code.");

        assertNotEquals(b1, null, "Object must not equal null.");
        assertNotEquals(b1, new Object(), "Object must not equal other classes.");
    }
}
