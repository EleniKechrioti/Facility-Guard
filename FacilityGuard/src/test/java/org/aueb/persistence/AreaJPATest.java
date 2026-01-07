package org.aueb.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.util.Address;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AreaJPATest extends JPATest {

    /**
     * Helper method: Creates a baseline scenario (Building -> Area -> Checkpoint)
     * and saves it to the base. Returns the Area for the test to use.
     */
    private Area createBasicScenario(String buildingName, String areaName) {
        Address addr = new Address("Test Street", "1", "Test City", "10000", "Greece");
        Building building = new Building(buildingName, addr);
        Area area = new Area(areaName, building);
        Checkpoint cp1 = new Checkpoint("Default CP");

        building.addArea(area);
        area.addCheckpoint(cp1);

        em.persist(building); // Cascades to Area & Checkpoint
        em.flush(); // Writing on the base to get IDs

        return area;
    }

    @Test
    @Transactional
    void createArea_withBuilding() {
        Address addr = new Address("Test Street", "1", "Test City", "10000", "Greece");
        Building persistentBuilding = new Building("Test Headquarters B", addr);

        Area newArea = new Area("Area 1", persistentBuilding);
        persistentBuilding.addArea(newArea);

        em.persist(persistentBuilding);
        em.flush();
        em.clear();

        Area retrievedArea = em.find(Area.class, newArea.getAreaId());

        assertNotNull(retrievedArea, "The Area entity must be retrieved successfully.");
        assertEquals("Area 1", retrievedArea.getName());
        assertNotNull(retrievedArea.getBuilding(), "The Building relationship must not be null.");
    }

    @Test
    @Transactional
    void areaToCheckpoint() {
        Area entranceArea = createBasicScenario("HQ C", "Entrance Area");

        Checkpoint cp2 = new Checkpoint("East Exit");
        entranceArea.addCheckpoint(cp2);

        em.persist(cp2);
        em.flush();
        em.clear();

        Area retrievedArea = em.find(Area.class, entranceArea.getAreaId());

        assertNotNull(retrievedArea.getCheckpoints());
        assertEquals(2, retrievedArea.getCheckpoints().size(), "Area must contain exactly 2 Checkpoints (1 from helper + 1 added).");
    }

    @Test
    @Transactional
    void areaToNeighbors() {
        Address addr = new Address("Test Street", "1", "Test City", "10000", "Greece");
        Building persistentBuilding = new Building("Test Headquarters D", addr);
        em.persist(persistentBuilding);

        Area classA = new Area("Class A", persistentBuilding);
        Area serverRoom = new Area("Server Room", persistentBuilding);
        Area classB = new Area("Class B", persistentBuilding);

        em.persist(classA);
        em.persist(serverRoom);
        em.persist(classB);
        em.flush();

        // Add Neighbors
        classA.addNeighbor(serverRoom);
        classA.addNeighbor(classB);

        em.merge(classA);
        em.flush();
        em.clear();

        Area retrievedClassA = em.find(Area.class, classA.getAreaId());

        assertEquals(2, retrievedClassA.getNeighbors().size(), "Class A must have 2 neighbors.");
        assertTrue(retrievedClassA.getNeighbors().stream().anyMatch(n -> n.getName().equals("Server Room")));
    }

    @Test
    @Transactional
    void testAreaUpdateName() {
        Area area = createBasicScenario("Update Building", "Class A");

        // Update Action
        String newName = "Class B";
        area.setName(newName);

        em.flush();
        em.clear();

        Area retrievedArea = em.find(Area.class, area.getAreaId());
        assertEquals(newName, retrievedArea.getName());
    }

    @Test
    @Transactional
    void testRemoveCheckpoint_fromArea() {
        Area area = createBasicScenario("Orphan Building", "Class A");
        int areaId = area.getAreaId();

        // We find the ID of the checkpoint made by the helper
        int checkpointId = area.getCheckpoints().iterator().next().getCheckpointId();

        em.clear(); // Clean to test the retrieval and remove

        Area managedArea = em.find(Area.class, areaId);
        Checkpoint checkpointToRemove = managedArea.getCheckpoints().iterator().next();

        // Remove Action
        managedArea.removeCheckpoint(checkpointToRemove);

        em.flush(); // Trigger Orphan Removal
        em.clear();

        Area finalArea = em.find(Area.class, areaId);
        assertEquals(0, finalArea.getCheckpoints().size());
        assertNull(em.find(Checkpoint.class, checkpointId), "Checkpoint should be deleted from DB.");
    }

    @Test
    @Transactional
    void testRemoveNeighbor() {
        Address addr = new Address("Test Street", "1", "Test City", "10000", "Greece");
        Building building = new Building("Neighbor Building", addr);
        em.persist(building);

        Area areaA = new Area("Class A", building);
        Area areaB = new Area("Class B", building);

        areaA.addNeighbor(areaB);

        em.persist(areaA);
        em.persist(areaB);
        em.flush();
        em.clear();

        Area managedAreaA = em.find(Area.class, areaA.getAreaId());
        Area managedAreaB = em.find(Area.class, areaB.getAreaId());

        managedAreaA.removeNeighbor(managedAreaB);

        em.flush();
        em.clear();

        Area finalAreaA = em.find(Area.class, areaA.getAreaId());
        assertEquals(0, finalAreaA.getNeighbors().size());
    }

    @Test
    @Transactional
    void testAreaDelete() {
        Area area = createBasicScenario("Delete Building", "Class A");
        int areaId = area.getAreaId();
        // The checkpoint ID made automatically
        int checkpointId = area.getCheckpoints().iterator().next().getCheckpointId();

        em.clear();

        // Delete Action
        Area managedArea = em.find(Area.class, areaId);
        managedArea.getBuilding().removeArea(managedArea);

        em.remove(managedArea);
        em.flush();
        em.clear();

        assertNull(em.find(Area.class, areaId), "The Area entity must be deleted.");
        assertNull(em.find(Checkpoint.class, checkpointId), "The Checkpoint must be deleted via cascade.");
    }

    @Test
    @Transactional
    public void fetchAreaWithBuildingAndCheckpoints() {
        createBasicScenario("Fetch Building", "Class A");
        em.clear();

        Query query = em.createQuery("select a from Area a " +
                "join fetch a.building b " +
                "left join fetch a.checkpoints cp " +
                "where b.name = :name");

        query.setParameter("name", "Fetch Building");
        List<Area> result = query.getResultList();

        assertTrue(result.size() >= 1);
        Area fetchedArea = result.get(0);

        assertNotNull(fetchedArea.getBuilding());
        assertEquals(1, fetchedArea.getCheckpoints().size());
    }

    @Test
    @Transactional
    void testLazyLoading_FailsOutOfContext() {
        Area area = createBasicScenario("Lazy Building", "Class A");
        em.clear();

        // Fetch Area (Checkpoints are LAZY by default)
        Area retrievedArea = em.find(Area.class, area.getAreaId());

        // Detach manually
        em.detach(retrievedArea);

        // Expect Failure
        assertThrows(LazyInitializationException.class, () -> {
            retrievedArea.getCheckpoints().size();
        });
    }
}