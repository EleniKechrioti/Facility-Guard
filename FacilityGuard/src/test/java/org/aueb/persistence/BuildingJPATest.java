package org.aueb.persistence;

import org.aueb.util.*;
import org.aueb.domain.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for verifying the persistence and retrieval of the Building entity.
 */
public class BuildingJPATest extends JPATest{

    /**
     * Tests the successful creation, persistence (with cascading Address), and retrieval of a Building.
     */
    @Test
    void createBuilding_withAddress() {
        Address testAddress = new Address("Evelpidon", "47", "Athens", "11362", "Greece");
        Building newBuilding = new Building("AUEB Building 1", testAddress);

        em.getTransaction().begin();
        em.persist(newBuilding);
        em.getTransaction().commit();

        em.clear(); /** Clear context  */

        Building retrievedBuilding = em.find(Building.class, newBuilding.getBuildingId());

        assertNotNull(retrievedBuilding, "The Building entity must be retrieved successfully.");
        assertEquals("AUEB Building 1", retrievedBuilding.getName());

        /**  Check if Address was saved via cascade  */
        assertNotNull(retrievedBuilding.getAddress(), "The associated Address must not be null.");
        assertEquals("Athens", retrievedBuilding.getAddress().getCity(), "Address city must match the original value.");
    }

    /**
     * Tests updating a non-key field (name) of the Building entity.
     */
    @Test
    void testBuildingUpdateName() {
        Address testAddress = new Address("Evelpidon", "47", "Athens", "11362", "Greece");
        Building newBuilding = new Building("AUEB", testAddress);

        em.getTransaction().begin();
        em.persist(newBuilding);
        em.getTransaction().commit();

        String updatedName = "AUEB Trias";
        em.getTransaction().begin();
        newBuilding.setName(updatedName);
        em.getTransaction().commit();

        em.clear();

        /** Assert: Retrieve and verify the update   */
        Building retrievedBuilding = em.find(Building.class, newBuilding.getBuildingId());
        assertEquals(updatedName, retrievedBuilding.getName(), "The Building name must be updated successfully.");
    }

    /**
     * Tests the deletion of the Building entity and ensures it is removed from the database.
     */
    @Test
    void testBuildingDelete() {
        Address testAddress = new Address("Test", "1", "Athens", "10000", "Greece");
        Building buildingToDelete = new Building("AUEB", testAddress);

        em.getTransaction().begin();
        em.persist(buildingToDelete);
        em.getTransaction().commit();

        int buildingId = buildingToDelete.getBuildingId();

        em.getTransaction().begin();
        Building managedBuilding = em.find(Building.class, buildingId);
        em.remove(managedBuilding);
        em.getTransaction().commit();

        assertNull(em.find(Building.class, buildingId), "The Building must be deleted from the database.");
    }

    /**
     * Tests the functionality of orphanRemoval=true by removing an Area
     * from the Building's collection and ensuring it is deleted from the database.
     */
    @Test
    void testRemoveArea() {
        Address address = new Address("Test St", "1", "City", "10000", "Greece");
        Building building = new Building("AUEB", address);
        Area areaToRemove = new Area("Class A", building);

        building.addArea(areaToRemove);

        em.getTransaction().begin();
        em.persist(building);
        em.getTransaction().commit();

        em.clear();

        int areaIdToRemove = areaToRemove.getAreaId();

        em.getTransaction().begin();
        Building managedBuilding = em.find(Building.class, building.getBuildingId());

        Area managedAreaToRemove = managedBuilding.getAreas().stream()
                .filter(a -> a.getAreaId() == areaIdToRemove)
                .findFirst().orElseThrow();

        managedBuilding.removeArea(managedAreaToRemove);
        em.getTransaction().commit();

        em.clear();

        assertNull(em.find(Area.class, areaIdToRemove),
                "The Area must be deleted from the DB due to orphanRemoval=true.");
    }

    /**
     * Tests that deleting the parent Building entity automatically deletes its child Areas
     */
    @Test
    void testBuildingDelete_andAreas() {
        Address address = new Address("Cascade St", "2", "City", "10000", "Greece");
        Building buildingToDelete = new Building("AUEB", address);
        Area childArea = new Area("Class A", buildingToDelete);

        buildingToDelete.addArea(childArea);

        em.getTransaction().begin();
        em.persist(buildingToDelete);
        em.getTransaction().commit();

        int buildingId = buildingToDelete.getBuildingId();
        int childAreaId = childArea.getAreaId();

        em.clear();

        em.getTransaction().begin();
        Building managedBuilding = em.find(Building.class, buildingId);
        em.remove(managedBuilding);
        em.getTransaction().commit();

        assertNull(em.find(Building.class, buildingId), "The Building must be deleted.");
        assertNull(em.find(Area.class, childAreaId), "The child Area must be deleted due to CascadeType.ALL.");
    }
}
