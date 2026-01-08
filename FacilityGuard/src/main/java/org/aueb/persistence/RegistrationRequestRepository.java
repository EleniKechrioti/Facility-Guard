package org.aueb.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.aueb.domain.RegistrationRequest;
import org.aueb.util.enumerations.ActivityStatus;

import java.util.List;

/**
 * Repository για την οντότητα RegistrationRequest.
 * Ακολουθεί το πρότυπο Panache Repository.
 */
@ApplicationScoped
public class RegistrationRequestRepository implements PanacheRepository<RegistrationRequest> {

    /**
     * Επιστρέφει όλες τις αιτήσεις που είναι σε κατάσταση PENDING.
     * Κριτήριο: status = ACTIVE και approved = FALSE.
     * Ταξινόμηση: Παλαιότερες αιτήσεις πρώτα (FIFO).
     */
    public List<RegistrationRequest> findPendingRequests() {
        return find("status = :status and approved = :approved",
                Sort.by("requestDate").ascending(),
                Parameters.with("status", ActivityStatus.Active)
                        .and("approved", false))
                .list();
    }

    /**
     * Βρίσκει όλες τις αιτήσεις ενός συγκεκριμένου χρήστη.
     * Χρήσιμο για να βλέπει ο χρήστης το ιστορικό του.
     * @param userId το ID του χρήστη
     */
    public List<RegistrationRequest> findByUserId(int userId) {
        return list("user.id", userId);
    }

    /**
     * Βρίσκει αν υπάρχει ΕΝΕΡΓΗ αίτηση για τον χρήστη.
     * Χρήσιμο για validation πριν επιτρέψουμε νέα αίτηση.
     */
    public boolean hasActiveRequest(int userId) {
        return find("user.id = ?1 and status = ?2", userId, ActivityStatus.Active).count() > 0;
    }
}