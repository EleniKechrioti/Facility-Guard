package org.aueb.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.aueb.domain.Area;
import org.aueb.domain.Building;
import org.aueb.domain.Checkpoint;
import org.aueb.persistence.AreaRepository;
import org.aueb.persistence.BuildingRepository;
import org.aueb.persistence.CheckpointRepository;
import org.aueb.util.Address;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

@QuarkusTest
public class FacilityServiceTest {

    @Inject FacilityService facilityService;

    @Inject BuildingRepository buildingRepo;
    @Inject AreaRepository areaRepo;
    @Inject CheckpointRepository checkpointRepo;
    @Inject EntityManager em;

    private Integer buildingId;

    @BeforeEach
    @Transactional
    void setup() {
        // Create a base building for tests
        Building b = new Building("Test Facility", new Address("PattisionTest", "12", "Athens", "11362", "Greece"));
        buildingRepo.persist(b);
        this.buildingId = b.getBuildingId();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Order matters for clean deletion
        checkpointRepo.deleteAll();
        areaRepo.deleteAll();
        buildingRepo.deleteAll();
    }

    /**
     * Test: Create a Building successfully.
     */
    @Test
    @Transactional
    void testCreateBuilding() {
        Building b = new Building("New HQ", new Address("Pattision", "12", "Athens", "11362", "Greece"));
        facilityService.createBuilding(b);

        Assertions.assertNotNull(b.getBuildingId());
        Assertions.assertEquals(2, buildingRepo.count()); // 1 from setup + 1 new
    }

    /**
     * Test: Add an Area to a Building.
     */
    @Test
    @Transactional
    void testAddAreaToBuilding() {
        Area area = new Area("Server Room", null);

        Area created = facilityService.addAreaToBuilding(buildingId, area);

        Assertions.assertNotNull(created.getAreaId());
        Assertions.assertEquals(buildingId, created.getBuilding().getBuildingId());
        Assertions.assertEquals(1, areaRepo.count());
    }

    /**
     * Test: Add Checkpoint to Area.
     */
    @Test
    @Transactional
    void testAddCheckpointToArea() {
        // Setup Area
        Area area = new Area("Lobby", null);
        facilityService.addAreaToBuilding(buildingId, area);
        Integer areaId = area.getAreaId();

        // Add Checkpoint via Service
        Checkpoint cp = new Checkpoint("Front Desk Reader");
        facilityService.addCheckpointToArea(buildingId, areaId, cp);

        // Assert
        Assertions.assertNotNull(cp.getCheckpointId());
        Assertions.assertEquals(areaId, cp.getArea().getAreaId());
    }

    /**
     * Test: Try to add Checkpoint when Building ID mismatches the Area's Building.
     * Should throw SecurityException.
     */
    @Test
    @Transactional
    void testAddCheckpoint_MismatchBuilding() {
        // Create Area in Building A (from setup)
        Area area = new Area("Lobby", null);
        facilityService.addAreaToBuilding(buildingId, area);

        // Create Building B
        Building otherBuilding = new Building("Other Place", new Address("PattisionTest2", "12", "Athens", "11362", "Greece"));
        buildingRepo.persist(otherBuilding);
        Integer otherId = otherBuilding.getBuildingId();

        // Try to add Checkpoint to Area(in A) but claiming it's in Building B
        Checkpoint cp = new Checkpoint("Hacker Reader");

        Assertions.assertThrows(SecurityException.class, () -> {
            facilityService.addCheckpointToArea(otherId, area.getAreaId(), cp);
        });
    }

    /**
     * Test: Connect Neighbors (Topology).
     * This is critical for AccessControlService.
     */
    @Test
    @Transactional
    void testConnectNeighbors() {
        // Create two areas
        Area a1 = facilityService.addAreaToBuilding(buildingId, new Area("Hallway", null));
        Area a2 = facilityService.addAreaToBuilding(buildingId, new Area("Office", null));

        // Connect them
        facilityService.connectNeighbors(a1.getAreaId(), a2.getAreaId());

        // Flush to DB to ensure relationships are saved
        em.flush();
        em.clear();

        // Reload and Verify
        Area reloadedA1 = areaRepo.findById(a1.getAreaId());
        Area reloadedA2 = areaRepo.findById(a2.getAreaId());

        Assertions.assertTrue(reloadedA1.isNeighborOf(reloadedA2), "A1 should have A2 as neighbor");
        Assertions.assertTrue(reloadedA2.isNeighborOf(reloadedA1), "A2 should have A1 as neighbor (Bi-directional)");
    }

    /**
     * Test: Cascading Delete.
     * Deleting a Building should delete its Areas and Checkpoints.
     */
    @Test
    @Transactional
    void testDeleteBuilding_Cascade() {
        // Build Hierarchy: Building -> Area -> Checkpoint
        Area area = facilityService.addAreaToBuilding(buildingId, new Area("Zone X", null));
        Checkpoint cp = new Checkpoint("Door X");
        facilityService.addCheckpointToArea(buildingId, area.getAreaId(), cp);

        Assertions.assertEquals(1, buildingRepo.count());
        Assertions.assertEquals(1, areaRepo.count());
        Assertions.assertEquals(1, checkpointRepo.count());

        // Delete Building
        boolean deleted = facilityService.deleteBuilding(buildingId);

        // Assert Everything is gone
        Assertions.assertTrue(deleted);
        Assertions.assertEquals(0, buildingRepo.count());
        Assertions.assertEquals(0, areaRepo.count());
        Assertions.assertEquals(0, checkpointRepo.count());
    }
}