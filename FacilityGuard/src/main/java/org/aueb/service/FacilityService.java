package org.aueb.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.aueb.domain.Area;
import org.aueb.domain.Building;
import org.aueb.domain.Checkpoint;
import org.aueb.persistence.AccessLogRepository;
import org.aueb.persistence.AreaRepository;
import org.aueb.persistence.BuildingRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class FacilityService {

    @Inject
    BuildingRepository buildingRepo;

    @Inject
    AreaRepository areaRepo;

    @Inject
    AccessLogRepository logRepo;

    /**
     * Επιστρέφει τον συνολικό αριθμό κινήσεων (Logs) σε ένα κτίριο για τη σημερινή μέρα.
     * Χρήσιμο για Dashboard (π.χ. "Κίνηση Σήμερα: 150 άτομα").
     */
    public long getTodayBuildingActivity(Integer buildingId) {
        // 1. Υπολογισμός αρχής της ημέρας
        Date startOfDay = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 2. Query μέσω Panache
        // "Βρες logs όπου το checkpoint ανήκει σε Area του Κτιρίου Χ και έγιναν μετά το πρωί"
        return logRepo.count("checkpoint.area.building.buildingId = ?1 and timestamp >= ?2",
                buildingId, startOfDay);
    }

    /**
     * Ελέγχει αν δύο περιοχές είναι άμεσα συνδεδεμένες (Neighbors).
     * Χρήσιμο για έλεγχο λογικής (π.χ. δεν μπορείς να πας από την είσοδο στο Server Room απευθείας).
     */
    public boolean areAreasConnected(Integer areaId1, Integer areaId2) {
        Area area1 = areaRepo.findById(areaId1);
        Area area2 = areaRepo.findById(areaId2);

        if (area1 == null || area2 == null) return false;

        // Χρησιμοποιούμε τη μέθοδο isNeighborOf που έχεις ήδη στην Entity
        return area1.isNeighborOf(area2);
    }

    /**
     * Φέρνει όλα τα Checkpoints ενός κτιρίου σε μια επίπεδη λίστα.
     * Χρήσιμο για μαζικούς ελέγχους ή "Lockdown" λειτουργίες.
     */
    public List<Checkpoint> getAllCheckpointsInBuilding(Integer buildingId) {
        Building building = buildingRepo.findById(buildingId);
        if (building == null) return Collections.emptyList();

        // Stream: Building -> Areas -> Checkpoints -> List
        return building.getAreas().stream()
                .flatMap(area -> area.getCheckpoints().stream())
                .collect(Collectors.toList());
    }
}