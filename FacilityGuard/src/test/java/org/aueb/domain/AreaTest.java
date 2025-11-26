package org.aueb.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Area domain entity.
 * This class verifies the correct functionality of constructors, attribute assignments,
 * and core domain logic related to location management and relationships, independent of the
 * persistence layer.
 */
public class AreaTest {

    /**
     * Helper method to create a test {@link Building} entity
     * @return a Building
     */
    private Building createTransientBuilding() {
        // We only need a placeholder Building, not a persistent one.
        return new Building("Transient Building", null);
    }

    /**
     * Checks the correlation between {@link Checkpoint} and {@link Area}
     */
    @Test
    void addCheckpointAndCheckBidirectionalLink() {
        Building building = createTransientBuilding();
        Area area = new Area("Lobby", building);
        Checkpoint cp1 = new Checkpoint("Reader A");

        area.addCheckpoint(cp1);

        assertEquals(1, area.getCheckpoints().size(), "Area must contain the new checkpoint.");
        assertEquals(area, cp1.getArea(), "Checkpoint must reference the correct Area (back-reference).");
    }

    /**
     * Checks the correlation between neighboring areas.
     */
    @Test
    void addNeighborAndCheckSymmetry() {
        Building building = createTransientBuilding();
        Area areaA = new Area("North Hall", building);
        Area areaB = new Area("South Hall", building);
        Area areaC = new Area("Isolation Zone", building);

        areaA.addNeighbor(areaB);

        /**  A must-have B as a neighbor   */
        assertEquals(1, areaA.getNeighbors().size(), "Area A must have one neighbor.");
        assertTrue(areaA.getNeighbors().contains(areaB), "Area A's set must contain Area B.");

        /**  B must-have A as a neighbor   */
        assertEquals(1, areaB.getNeighbors().size(), "Area B must have one neighbor (symmetrical).");
        assertTrue(areaB.getNeighbors().contains(areaA), "Area B's set must contain Area A.");

        /** Area C has no neighbor    */
        assertFalse(areaA.isNeighborOf(areaC), "Area A must NOT recognize Area C as neighbor.");
        assertFalse(areaA.isNeighborOf(null), "Should return false for null input.");
    }

    /**
     * Checks object's equality before persistence.
     */
    @Test
    void checkAreaEqualityBeforePersistence() {
        Building building = createTransientBuilding();
        Area area1 = new Area("Main Zone", building);
        Area area2 = new Area("Main Zone", building);

        //We don't persist, so IDs are 0.
        assertEquals(area1, area2, "Two distinct Area objects should not be equal before IDs are set.");
        area2.setName("Main Zone 2");
        assertNotEquals(area1, area2, "Two distinct Area objects should not be equal before IDs are set.");
    }

    /**
     * Checks if a checkpoint can be retrieved by name.
     */
    @Test
    void getCheckpointByName() {
        Building building = createTransientBuilding();
        Area area = new Area("Lobby", building);
        Checkpoint cp1 = new Checkpoint("Main Reader North");
        Checkpoint cp2 = new Checkpoint("Exit Reader South");

        area.addCheckpoint(cp1);
        area.addCheckpoint(cp2);

        Checkpoint foundCp = area.getCheckpointByName("Exit Reader South");
        Checkpoint notFoundCp = area.getCheckpointByName("Non-existent CP");

        assertNotNull(foundCp, "Should find the Checkpoint with the exact name.");
        assertEquals(cp2.getName(), foundCp.getName(), "The retrieved Checkpoint should be correct.");
        assertNull(notFoundCp, "Should return null if the Checkpoint name does not exist.");
    }
}