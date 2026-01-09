package org.aueb.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.aueb.domain.Permission;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PermissionRepository implements PanacheRepositoryBase<Permission, Integer> {

    /**
     * Βρίσκει όλα τα δικαιώματα (Permissions) που ανήκουν σε μία συγκεκριμένη κάρτα.
     * Χρήσιμο για το endpoint GET /cards/{id}/permissions
     */
    public List<Permission> findByCardId(Integer cardId) {
        // Στο Permission entity, η σχέση ονομάζεται "accessCard".
        return list("accessCard.cardId = ?1", cardId);
    }

    /**
     * Βρίσκει ένα συγκεκριμένο δικαίωμα βάσει κάρτας και περιοχής.
     * Χρήσιμο για να ελέγξουμε αν υπάρχει ήδη δικαίωμα πριν προσθέσουμε νέο.
     */
    public Optional<Permission> findByCardAndArea(Integer cardId, Integer areaId) {
        return find("accessCard.cardId = ?1 and area.areaId = ?2", cardId, areaId)
                .firstResultOptional();
    }
}