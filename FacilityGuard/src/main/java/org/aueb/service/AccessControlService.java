package org.aueb.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.aueb.domain.AccessCard;
import org.aueb.domain.AccessLog;
import org.aueb.domain.Checkpoint;
import org.aueb.domain.Area;
import org.aueb.persistence.AccessCardRepository;
import org.aueb.persistence.AccessLogRepository;
import org.aueb.persistence.CheckpointRepository;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;

@ApplicationScoped
public class AccessControlService {

    @Inject
    AccessCardRepository cardRepo;

    @Inject
    CheckpointRepository checkpointRepo;

    @Inject
    AccessLogRepository logRepo;

    /**
     * Ελέγχει αν μια κάρτα έχει δικαίωμα πρόσβασης σε ένα σημείο.
     * @param cardId Το ID της κάρτας.
     * @param checkpointId Το ID του σημείου ελέγχου.
     * @param accessType Ο τύπος κίνησης (ENTRY/EXIT).
     */
    @Transactional
    public boolean requestAccess(Integer cardId, Integer checkpointId, AccessType accessType) {
        // 1. Ανάκτηση οντοτήτων
        AccessCard card = cardRepo.findById(cardId);
        Checkpoint checkpoint = checkpointRepo.findById(checkpointId);

        // Αν δεν υπάρχουν, επιστροφή false (χωρίς log, καθώς λείπουν τα στοιχεία)
        if (card == null || checkpoint == null) {
            return false;
        }

        // 2. Έλεγχος εγκυρότητας Κάρτας
        // Χρησιμοποιούμε την έτοιμη business logic της AccessCard (expiration + status)
        if (!card.isValid()) {
            logAccess(card, checkpoint, PermissionType.AccessDenied, accessType);
            return false;
        }

        // 3. Έλεγχος Δικαιωμάτων (Permission Check)
        // Βρίσκουμε την περιοχή που φυλάει το Checkpoint
        Area targetArea = checkpoint.getArea();

        if (targetArea == null) {
            return false; // Ορφανό checkpoint
        }

        // Ψάχνουμε στη λίστα Permissions της κάρτας αν υπάρχει άδεια για τη συγκεκριμένη περιοχή
        // Υποθέτουμε ότι η κλάση Permission έχει μέθοδο getArea()
        boolean hasPermission = card.getPermissions().stream()
                .anyMatch(perm -> perm.getArea().getAreaId() == targetArea.getAreaId());

        // 4. Καταγραφή αποτελέσματος
        PermissionType result = hasPermission ? PermissionType.AccessGranted : PermissionType.AccessDenied;
        logAccess(card, checkpoint, result, accessType);

        return hasPermission;
    }

    /**
     * Βοηθητική μέθοδος για την καταγραφή στο Log.
     */
    private void logAccess(AccessCard card, Checkpoint cp, PermissionType result, AccessType type) {
        // Χρησιμοποιούμε τον Constructor της AccessLog που έφτιαξες προηγουμένως
        // Αυτός βάζει αυτόματα το Timestamp
        AccessLog log = new AccessLog(result, type, card, cp);

        logRepo.persist(log);
    }
}