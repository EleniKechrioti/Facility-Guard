package org.aueb.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CheckpointTest {

    @Test
    void testConstructorAndGetters() {
        Checkpoint cp = new Checkpoint("Gate A");
        assertEquals("Gate A", cp.getName());
    }

    @Test
    void testSetName() {
        Checkpoint cp = new Checkpoint("Old Name");
        cp.setName("New Name");
        assertEquals("New Name", cp.getName());
    }

    @Test
    void testEqualsBeforePersist() {
        Checkpoint cp1 = new Checkpoint("Reader 1");
        Checkpoint cp2 = new Checkpoint("Reader 1");

        assertEquals(cp1, cp2);  /** equal by name before persist  */
    }

    @Test
    void testSetArea() {
        Building b = new Building("HQ", null);
        Area a = new Area("Lab", b);

        Checkpoint cp = new Checkpoint("Reader");
        cp.setArea(a);

        assertEquals(a, cp.getArea());
    }
}
