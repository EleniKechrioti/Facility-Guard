package org.aueb.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.aueb.domain.*;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for verifying the persistence and relationships of the Area entity.
 */
public class AreaJPATest extends JPATest{


    /**
     * Tests the successful creation and retrieval of an Area.
     */
    @Test
    void createArea_withBuilding() {
        Building persistentBuilding = em.find(Building.class, this.testBuilding.getBuildingId());
        Area newArea = new Area("Area 1", persistentBuilding);

        em.getTransaction().begin();
        em.persist(newArea);
        em.getTransaction().commit();

        em.clear();

        Area retrievedArea = em.find(Area.class, newArea.getAreaId());

        assertNotNull(retrievedArea, "The Area entity must be retrieved successfully.");
        assertEquals("Area 1", retrievedArea.getName());

        assertNotNull(retrievedArea.getBuilding(), "The Building relationship must not be null.");
        assertEquals(persistentBuilding.getBuildingId(), retrievedArea.getBuilding().getBuildingId(),
                "The retrieved Building ID must match the parent Building ID.");
    }

    /**
     * Tests the persistence between Area and Checkpoint.
     */
    @Test
    void areaToCheckpoint() {
        Building persistentBuilding = em.find(Building.class, testBuilding.getBuildingId());
        Area entranceArea = new Area("Area 1", persistentBuilding);

        Checkpoint cp1 = new Checkpoint("West Entrance");
        Checkpoint cp2 = new Checkpoint("East Exit");

        entranceArea.addCheckpoint(cp1);
        entranceArea.addCheckpoint(cp2);

        em.getTransaction().begin();
        em.persist(entranceArea);
        em.getTransaction().commit();

        em.clear();

        Area retrievedArea = em.find(Area.class, entranceArea.getAreaId());

        assertNotNull(retrievedArea.getCheckpoints(), "The Checkpoints collection must not be null.");
        assertEquals(2, retrievedArea.getCheckpoints().size(), "Area must contain exactly 2 Checkpoints.");

        // Assert the back reference from Checkpoint to Area
        Checkpoint retrievedCp1 = retrievedArea.getCheckpoints().stream()
                .filter(cp -> cp.getName().equals("West Entrance"))
                .findFirst().orElse(null);

        assertNotNull(retrievedCp1, "Checkpoint must be found.");
        assertEquals(retrievedArea.getAreaId(), retrievedCp1.getArea().getAreaId(),
                "The Checkpoint's area reference must point back to the Area.");
    }

    /**
     * Tests neighboring areas.
     */
    @Test
    void areaToNeighbors() {
        Building persistentBuilding = em.find(Building.class, testBuilding.getBuildingId());
        Area classA = new Area("Class A", persistentBuilding);
        Area serverRoom = new Area("Server Room", persistentBuilding);
        Area classB = new Area("Class B", persistentBuilding);

        em.getTransaction().begin();
        em.persist(classA);
        em.persist(serverRoom);
        em.persist(classB);
        em.getTransaction().commit();

        em.getTransaction().begin();

        Area managedCorridor = em.find(Area.class, classA.getAreaId());
        Area managedServerRoom = em.find(Area.class, serverRoom.getAreaId());
        Area managedOfficeA = em.find(Area.class, classB.getAreaId());

        managedCorridor.addNeighbor(managedServerRoom);
        managedCorridor.addNeighbor(managedOfficeA);

        em.merge(managedCorridor);

        em.getTransaction().commit();
        em.clear();

        Area retrievedClassA = em.find(Area.class, classA.getAreaId());
        Area retrievedServerRoom = em.find(Area.class, serverRoom.getAreaId());
        Area retrievedClassB = em.find(Area.class, classB.getAreaId());

        assertNotNull(retrievedClassA, "Class A must be retrieved");

        assertEquals(2, retrievedClassA.getNeighbors().size(),
                "Class A must have 2 neighbors.");

        assertEquals(1, retrievedServerRoom.getNeighbors().size(),
                "Server Room must have 1 neighbor (class A).");

        assertEquals(1, retrievedClassB.getNeighbors().size(),
                "Class B must have 1 neighbor (Class A).");

        assertTrue(retrievedClassA.getNeighbors().contains(retrievedServerRoom),
                "Server room must be in the neighbor's list of Class A.");

        assertTrue(retrievedServerRoom.getNeighbors().contains(retrievedClassA),
                "Class A must be in the neighbor's list of Server Room.");
    }

    /**
     * Tests a simple update operation on a non-key field of the Area entity.
     */
    @Test
    void testAreaUpdateName() {
        Building persistentBuilding = em.find(Building.class, testBuilding.getBuildingId());
        Area area = new Area("Class A", persistentBuilding);

        em.getTransaction().begin();
        em.persist(area);
        em.getTransaction().commit();

        String newName = "Class B";
        em.getTransaction().begin();
        area.setName(newName);
        em.getTransaction().commit();

        em.clear();

        Area retrievedArea = em.find(Area.class, area.getAreaId());

        assertNotNull(retrievedArea, "The Area must still exist.");
        assertEquals(newName, retrievedArea.getName(), "The Area name must be updated successfully.");
    }

    /**
     * Tests the functionality of orphanRemoval=true by removing a Checkpoint
     * from the Area's collection and ensuring it is deleted from the database.
     */
    @Test
    void testRemoveCheckpoint_fromArea() {
        Building persistentBuilding = em.find(Building.class, testBuilding.getBuildingId());
        Area area = new Area("Class A", persistentBuilding);
        Checkpoint cp1 = new Checkpoint("Checkpoint A");

        area.addCheckpoint(cp1);

        em.getTransaction().begin();
        em.persist(area);
        em.getTransaction().commit();

        em.clear();

        Area managedArea = em.find(Area.class, area.getAreaId());
        int checkpointId = managedArea.getCheckpoints().iterator().next().getCheckpointId();

        em.getTransaction().begin();
        Checkpoint checkpointToRemove = em.find(Checkpoint.class, checkpointId);
        managedArea.removeCheckpoint(checkpointToRemove);
        em.getTransaction().commit();

        em.clear();

        Area finalArea = em.find(Area.class, area.getAreaId());

        assertEquals(0, finalArea.getCheckpoints().size(), "The Checkpoint should be removed from the collection.");
        assertNull(em.find(Checkpoint.class, checkpointId), "The orphaned Checkpoint must be deleted from the DB.");
    }

    /**
     * Tests the correct removal of the symmetrical Many-to-Many neighbor relationship.
     */
    @Test
    void testRemoveNeighbor() {
        Building persistentBuilding = em.find(Building.class, testBuilding.getBuildingId());
        Area areaA = new Area("Class A", persistentBuilding);
        Area areaB = new Area("Class B", persistentBuilding);

        areaA.addNeighbor(areaB);

        em.getTransaction().begin();
        em.persist(areaA);
        em.persist(areaB);
        em.getTransaction().commit();

        em.clear();

        Area managedAreaA = em.find(Area.class, areaA.getAreaId());
        Area managedAreaB = em.find(Area.class, areaB.getAreaId());

        em.getTransaction().begin();
        managedAreaA.removeNeighbor(managedAreaB); // Helper method updates both sets
        em.getTransaction().commit();

        em.clear();

        Area finalAreaA = em.find(Area.class, areaA.getAreaId());
        Area finalAreaB = em.find(Area.class, areaB.getAreaId());

        assertEquals(0, finalAreaA.getNeighbors().size(), "Area A should have no neighbors left.");
        assertEquals(0, finalAreaB.getNeighbors().size(), "Area B should have no neighbors left.");
    }

    /**
     * Tests the cascading deletion of the Area entity and its dependent children (Checkpoints).
     */
    @Test
    void testAreaDelete() {
        Building persistentBuilding = em.find(Building.class, testBuilding.getBuildingId());
        Area areaToDelete = new Area("Class A", persistentBuilding);
        Checkpoint cp1 = new Checkpoint("Checkpoint A");

        areaToDelete.addCheckpoint(cp1);

        em.getTransaction().begin();
        em.persist(areaToDelete);
        em.getTransaction().commit();

        int areaId = areaToDelete.getAreaId();
        int checkpointId = cp1.getCheckpointId();

        em.clear();

        em.getTransaction().begin();
        Area managedArea = em.find(Area.class, areaId);
        em.remove(managedArea); // Deletes Area and cascades to Checkpoints
        em.getTransaction().commit();

        em.clear();

        assertNull(em.find(Area.class, areaId), "The Area entity must be deleted.");
        assertNull(em.find(Checkpoint.class, checkpointId),
                "The Checkpoint must be deleted via cascade operation.");
    }

    /**
     * Tests loading the Area entity along with its Building and Checkpoints
     * in a single query using JOIN FETCH
     */
    @Test
    public void fetchAreaWithBuildingAndCheckpoints() {
        Building currentBuilding = em.find(Building.class, this.testBuilding.getBuildingId());

        Area testArea = new Area("Class A", currentBuilding);
        Checkpoint cp1 = new Checkpoint("Checkpoint A");

        testArea.addCheckpoint(cp1);

        em.getTransaction().begin();
        em.persist(testArea);
        em.getTransaction().commit();

        em.clear();

        Query query = em.createQuery("select a from Area a " +
                "join fetch a.building b " +
                "left join fetch a.checkpoints cp " +
                "where b.name = :name");

        query.setParameter("name", currentBuilding.getName());
        List<Area> result = query.getResultList();


        assertTrue(result.size() >= 1, "At least one Area must be found after local persistence.");

        Area fetchedArea = result.get(0);

        assertNotNull(fetchedArea.getBuilding(), "Building must be retrieved.");

        assertFalse(fetchedArea.getCheckpoints().isEmpty(),
                "Checkpoint set must be retrieved and have 1 element.");
        assertEquals(1, fetchedArea.getCheckpoints().size(),
                "Checkpoint set must be retrieved and have exactly 1 element.");
    }

    /**
     * Tests that accessing a LAZY collection after the EntityManager (session) is closed
     * correctly throws a LazyInitializationException.
     */
    @Test
    void testLazyLoading_FailsOutOfContext() {
        Building persistentBuilding = em.find(Building.class, this.testBuilding.getBuildingId());
        Area area = new Area("Class A", persistentBuilding);
        area.addCheckpoint(new Checkpoint("Checkpoint A"));

        em.getTransaction().begin();
        em.persist(area);
        em.getTransaction().commit();

        int areaId = area.getAreaId();

        em.close();

        //Use a new entity manager for the retrieval
        EntityManager em2 = JPAUtil.getCurrentEntityManager();
        Area retrievedArea = em2.find(Area.class, areaId);
        em2.close();

        assertThrows(
                LazyInitializationException.class,
                () -> {
                    retrievedArea.getCheckpoints().size();
                },
                "Accessing a LAZY collection on a detached entity must throw LazyInitializationException."
        );
        em = JPAUtil.getCurrentEntityManager();
    }
}