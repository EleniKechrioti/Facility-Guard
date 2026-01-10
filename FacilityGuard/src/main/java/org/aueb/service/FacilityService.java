package org.aueb.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.aueb.domain.Area;
import org.aueb.domain.Building;
import org.aueb.domain.Checkpoint;
import org.aueb.persistence.AreaRepository;
import org.aueb.persistence.BuildingRepository;
import org.aueb.persistence.CheckpointRepository;

import java.util.List;

@ApplicationScoped
public class FacilityService {

    @Inject BuildingRepository buildingRepo;
    @Inject AreaRepository areaRepo;
    @Inject CheckpointRepository checkpointRepo;

    /**
     * Creates and persists a new Building.
     */
    @Transactional
    public Building createBuilding(Building building) {
        if (building == null) throw new IllegalArgumentException("Building cannot be null");
        buildingRepo.persist(building);
        return building;
    }

    /**
     * Creates a new Area and links it to an existing Building.
     */
    @Transactional
    public Area addAreaToBuilding(Integer buildingId, Area area) {
        Building building = buildingRepo.findById(buildingId);
        if (building == null) {
            throw new IllegalArgumentException("Building with ID " + buildingId + " not found.");
        }

        // Domain Logic: Link objects
        building.addArea(area);

        // Persist
        areaRepo.persist(area);
        return area;
    }

    /**
     * Creates a Checkpoint and links it to an existing Area.
     * Validates that the Area belongs to the correct Building (if provided in context).
     */
    @Transactional
    public Checkpoint addCheckpointToArea(Integer buildingId, Integer areaId, Checkpoint checkpoint) {
        Building building = buildingRepo.findById(buildingId);
        Area area = areaRepo.findById(areaId);

        if (building == null) throw new IllegalArgumentException("Building not found");
        if (area == null) throw new IllegalArgumentException("Area not found");

        // Security/Consistency Check: Ensure Area is physically inside the Building
        if (area.getBuilding().getBuildingId() != buildingId) {
            throw new SecurityException("Mismatch: Area does not belong to the specified Building.");
        }

        // Link and Persist
        area.addCheckpoint(checkpoint);
        checkpointRepo.persist(checkpoint);
        return checkpoint;
    }

    /**
     * Establishes a neighbor relationship between two Areas.
     * Necessary for the AccessControlService topology checks.
     */
    @Transactional
    public void connectNeighbors(Integer areaId1, Integer areaId2) {
        Area area1 = areaRepo.findById(areaId1);
        Area area2 = areaRepo.findById(areaId2);

        if (area1 == null || area2 == null) {
            throw new IllegalArgumentException("One or both areas not found.");
        }

        // Domain Logic: Bi-directional link
        area1.addNeighbor(area2);

        // No explicit persist needed for managed entities, but ensured by Transaction commit.
    }

    /**
     * Deletes a building and (via Cascade) all its Areas and Checkpoints.
     */
    @Transactional
    public boolean deleteBuilding(Integer buildingId) {
        return buildingRepo.deleteById(buildingId);
    }

    // --- Read Operations ---

    public List<Building> getAllBuildings() {
        return buildingRepo.listAll();
    }

    public Building findBuildingById(Integer id) {
        return buildingRepo.findById(id);
    }

    public Area findAreaById(Integer id) {
        return areaRepo.findById(id);
    }

    public List<Checkpoint> getCheckpointsOfArea(Integer areaId) {
        return checkpointRepo.fetchByArea(areaId);
    }

    /**
     * Removes the neighbor relationship between two Areas.
     */
    @Transactional
    public void disconnectNeighbors(Integer areaId1, Integer areaId2) {
        Area a1 = areaRepo.findById(areaId1);
        Area a2 = areaRepo.findById(areaId2);

        if (a1 == null || a2 == null) throw new IllegalArgumentException("Area not found");

        // Η μέθοδος removeNeighbor (στο Domain) καθαρίζει και τις δύο πλευρές
        a1.removeNeighbor(a2);
    }
}