package org.aueb.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import org.aueb.domain.AccessCard;
import org.aueb.util.enumerations.ActivityStatus;

import java.util.Date;
import java.util.List;

@ApplicationScoped
public class AccessCardRepository implements PanacheRepository<AccessCard> {

    /**
     * Βρίσκει όλες τις κάρτες που είναι αυτή τη στιγμή ΕΝΕΡΓΕΣ.
     */
    public List<AccessCard> findActiveCards() {
        return find("status", ActivityStatus.Active).list();
    }

    /**
     * Βρίσκει την κάρτα που ανήκει σε συγκεκριμένο User ID.
     * Επειδή η σχέση είναι 1-προς-1, επιστρέφουμε ένα αντικείμενο (όχι λίστα).
     */
    public AccessCard findByUserId(int userId) {
        // "user.id" αναφέρεται στο πεδίο user της AccessCard και στο PK του User
        return find("user.id", userId).firstResult();
    }

    /**
     * Βρίσκει κάρτες που λήγουν ΠΡΙΝ από μια συγκεκριμένη ημερομηνία
     * και είναι ακόμα ενεργές.
     * Χρήσιμο για να βγάλει ο Admin μια αναφορά με κάρτες που πρέπει να ανανεωθούν.
     */
    public List<AccessCard> findExpiringSoon(Date limitDate) {
        return find("expirationDate < :date and status = :status",
                Parameters.with("date", limitDate)
                        .and("status", ActivityStatus.Active))
                .list();
    }
}