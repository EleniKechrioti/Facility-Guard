package org.aueb.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.persistence.*;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;

import java.util.Date;
import java.util.Optional;

@ApplicationScoped
public class AccessControlService {

    @Inject AccessCardRepository cardRepo;
    @Inject CheckpointRepository checkpointRepo;
    @Inject PermissionRepository permissionRepo;
    @Inject AccessLogRepository logRepo;
    @Inject AlertRepository alertRepo;

    /**
     * The core method for processing access requests.
     * It executes a security pipeline with the following steps:
     *
     * 1. **Card Validation**: Checks if the card is active and not expired.
     * 2. **Anti-Passback**: Ensures temporal consistency (e.g., cannot Enter if already Inside).
     * 3. **Topology Check**: Ensures spatial consistency (e.g., can only move between neighboring areas).
     * 4. **Authorization**: Verifies specific permissions for the target Area.
     * 5. **Auditing**: Logs every attempt (Granted or Denied) to the database.
     * 6. **Alerting**: Triggers security alerts for denied access or policy violations.
     *
     * @param cardId       The ID of the access card.
     * @param checkpointId The ID of the checkpoint where the card was presented.
     * @param accessType   The type of movement (In or Out).
     * @return true if access is granted, false otherwise.
     */

    @Transactional
    public boolean requestAccess(Integer cardId, Integer checkpointId, AccessType accessType) {
        // 1. Retrieve Data/Entities
        AccessCard card = cardRepo.findById(cardId);
        Checkpoint checkpoint = checkpointRepo.findById(checkpointId);

        if (card == null || checkpoint == null) return false;

        boolean accessGranted = false;
        String denyReason = "";

        // 2. Card Validity Check
        if (!card.isValid()) {
            denyReason = "Card is Expired or Inactive";
        } else {
            // DETERMINE CURRENT LOCATION
            // Retrieve the user's last known position based on logs
            Optional<AccessLog> lastLogOpt = logRepo.findLastSuccessfulLog(cardId);

            Area currentArea = null; // null indicates "Outside"
            if (lastLogOpt.isPresent() && lastLogOpt.get().getAccessType() == AccessType.In) {
                // If the last movement was IN, the user is currently inside that area
                currentArea = lastLogOpt.get().getCheckpoint().getArea();
            }

            // ANTI-PASSBACK CHECK
            boolean passbackViolation = false;

            if (lastLogOpt.isPresent()) {
                AccessType lastType = lastLogOpt.get().getAccessType();

                // Case: User is INSIDE and attempts to Enter again
                if (lastType == AccessType.In && accessType == AccessType.In) {

                    // REFINEMENT: Check if attempting to enter the SAME area
                    Integer lastAreaId = lastLogOpt.get().getCheckpoint().getArea().getAreaId();
                    Integer targetAreaId = checkpoint.getArea().getAreaId();

                    if (lastAreaId.equals(targetAreaId)) {
                        passbackViolation = true;
                        denyReason = "Anti-Passback Violation: Card is already INSIDE this area";
                    }
                    // If it is a different area, allow it to proceed
                    // to the Topology Check below.
                }

                // Case: User is OUTSIDE and attempts to Exit again (Always invalid)
                else if (lastType == AccessType.Out && accessType == AccessType.Out) {
                    passbackViolation = true;
                    denyReason = "Anti-Passback Violation: Card is already OUTSIDE";
                }
            } else {
                // Case: No history exists (first use) and attempts to EXIT
                if (accessType == AccessType.Out) {
                    passbackViolation = true;
                    denyReason = "Anti-Passback Violation: Cannot EXIT without ENTRY history";
                }
            }

            if (passbackViolation) {
                accessGranted = false;
            } else {
                // TOPOLOGY / NEIGHBOR CHECK
                // Verify if movement from CurrentArea to TargetArea is allowed
                Area targetArea = checkpoint.getArea();
                boolean topologyViolation = false;

                // This check applies only if the user is already INSIDE (currentArea != null)
                // and attempting to enter another area (AccessType.In).
                // If outside (null), we assume entry via a main entrance.
                if (accessType == AccessType.In && currentArea != null) {

                    // Check if the target area is a NEIGHBOR of the current area
                    // (Prerequisite: Neighbors must be defined in the Area entity setup)
                    if (!currentArea.isNeighborOf(targetArea)) {
                        topologyViolation = true;
                        denyReason = "Topology Violation: Cannot move directly from '" +
                                currentArea.getName() + "' to '" + targetArea.getName() +
                                "' (Not Neighbors).";
                    }
                }

                if (topologyViolation) {
                    accessGranted = false;
                } else {
                    // PERMISSION CHECK
                    // If physical location checks pass, verify access rights
                    Optional<Permission> permissionOpt = permissionRepo.findByCardAndArea(cardId, targetArea.getAreaId());

                    if (permissionOpt.isPresent()) {
                        if (permissionOpt.get().getAccessGranted() == PermissionType.AccessGranted) {
                            accessGranted = true;
                        } else {
                            denyReason = "Permission explicitly set to DENIED for this Area";
                        }
                    } else {
                        denyReason = "No Permission found for Area: " + targetArea.getName();
                    }
                }
            }
        }

        // 3. Auditing / Logging
        PermissionType resultType = accessGranted ? PermissionType.AccessGranted : PermissionType.AccessDenied;
        AccessLog log = new AccessLog(resultType, accessType, card, checkpoint);
        log.setTimestamp(new Date());

        logRepo.persist(log);
        logRepo.flush(); // Important: Flushes data to DB immediately for subsequent queries

        // 4. Alerting (if denied)
        if (!accessGranted) {
            String alertMsg = String.format("Access Denied at '%s'. Reason: %s",
                    checkpoint.getName(), denyReason);

            Alert alert = new Alert(new Date(), alertMsg);
            alert.setAccessLog(log);
            alertRepo.persist(alert);
        }

        return accessGranted;
    }

    /**
     * Retrieves the current Area where the cardholder is located.
     * Returns null if the user is outside the facility.
     */
    public Area findCurrentLocation(Integer cardId) {
        Optional<AccessLog> lastLogOpt = logRepo.findLastSuccessfulLog(cardId);

        if (lastLogOpt.isPresent() && lastLogOpt.get().getAccessType() == AccessType.In) {
            // Last movement was IN, so the user is currently inside that area
            return lastLogOpt.get().getCheckpoint().getArea();
        }

        // If last movement was OUT or no history exists, user is "Outside"
        return null;
    }
}