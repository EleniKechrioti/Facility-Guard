package org.aueb.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.util.Address;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for verifying the persistence and retrieval of the Building entity.
 */
@QuarkusTest
public class BuildingJPATest extends JPATest {

    /**
     * Tests the successful creation, persistence (with cascading Address), and retrieval of a Building.
     */
    @Test
    @Transactional
    void createBuilding_withAddress() {
        Address testAddress = new Address("Evelpidon", "47", "Athens", "11362", "Greece");
        Building newBuilding = new Building("AUEB Building 1", testAddress);

        em.persist(newBuilding);
        em.flush(); // Writing on the Base
        em.clear(); // Clear cache

        Building retrievedBuilding = em.find(Building.class, newBuilding.getBuildingId());

        assertNotNull(retrievedBuilding, "The Building entity must be retrieved successfully.");
        assertEquals("AUEB Building 1", retrievedBuilding.getName());

        /* Check if Address was saved via cascade (Assuming Address is Embeddable or OneToOne Cascade) */
        assertNotNull(retrievedBuilding.getAddress(), "The associated Address must not be null.");
        assertEquals("Athens", retrievedBuilding.getAddress().getCity(), "Address city must match the original value.");
    }

    /**
     * Tests updating a non-key field (name) of the Building entity.
     */
    @Test
    @Transactional
    void testBuildingUpdateName() {
        Address testAddress = new Address("Evelpidon", "47", "Athens", "11362", "Greece");
        Building newBuilding = new Building("AUEB", testAddress);

        em.persist(newBuilding);
        em.flush();

        String updatedName = "AUEB Trias";

        newBuilding.setName(updatedName);

        em.flush(); // We send the SQL update
        em.clear(); // Clean to pull again

        /* Assert: Retrieve and verify the update */
        Building retrievedBuilding = em.find(Building.class, newBuilding.getBuildingId());
        assertEquals(updatedName, retrievedBuilding.getName(), "The Building name must be updated successfully.");
    }

    /**
     * Tests the deletion of the Building entity and ensures it is removed from the database.
     */
    @Test
    @Transactional
    void testBuildingDelete() {
        Address testAddress = new Address("Test", "1", "Athens", "10000", "Greece");
        Building buildingToDelete = new Building("AUEB", testAddress);

        em.persist(buildingToDelete);
        em.flush();

        int buildingId = buildingToDelete.getBuildingId();
        em.clear();

        // Delete
        Building managedBuilding = em.find(Building.class, buildingId);
        em.remove(managedBuilding);

        em.flush();
        em.clear();

        assertNull(em.find(Building.class, buildingId), "The Building must be deleted from the database.");
    }

    /**
     * Tests the functionality of orphanRemoval=true by removing an Area
     * from the Building's collection and ensuring it is deleted from the database.
     */
    @Test
    @Transactional
    void testRemoveArea() {
        Address address = new Address("Test St", "1", "City", "10000", "Greece");
        Building building = new Building("Orphan Building", address);
        Area areaToRemove = new Area("Class A", building);

        building.addArea(areaToRemove);

        em.persist(building);
        em.flush();

        int areaIdToRemove = areaToRemove.getAreaId();
        int buildingId = building.getBuildingId();
        em.clear();

        // Retrieve managed entities
        Building managedBuilding = em.find(Building.class, buildingId);

        // Find the specific area in the collection to remove
        Area managedAreaToRemove = managedBuilding.getAreas().stream()
                .filter(a -> a.getAreaId() == areaIdToRemove)
                .findFirst().orElseThrow();

        // Helper method handles both sides of relationship
        managedBuilding.removeArea(managedAreaToRemove);

        em.flush(); // Trigger orphan removal DELETE
        em.clear();

        assertNull(em.find(Area.class, areaIdToRemove),
                "The Area must be deleted from the DB due to orphanRemoval=true.");
    }

    /**
     * Tests that deleting the parent Building entity automatically deletes its child Areas
     */
    @Test
    @Transactional
    void testBuildingDelete_andAreas() {
        Address address = new Address("Cascade St", "2", "City", "10000", "Greece");
        Building buildingToDelete = new Building("Cascade Building", address);
        Area childArea = new Area("Class A", buildingToDelete);

        buildingToDelete.addArea(childArea);

        em.persist(buildingToDelete);
        em.flush();

        int buildingId = buildingToDelete.getBuildingId();
        int childAreaId = childArea.getAreaId();
        em.clear();

        // Retrieve and Delete Parent
        Building managedBuilding = em.find(Building.class, buildingId);
        em.remove(managedBuilding);

        em.flush(); // Trigger cascading deletes
        em.clear();

        assertNull(em.find(Building.class, buildingId), "The Building must be deleted.");
        assertNull(em.find(Area.class, childAreaId), "The child Area must be deleted due to CascadeType.ALL.");
    }
}