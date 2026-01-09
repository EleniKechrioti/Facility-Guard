package org.aueb.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.aueb.domain.AccessLog;
import org.aueb.util.enumerations.PermissionType;

import java.util.Date;
import java.util.List;

@RequestScoped
public class AccessLogRepository
        implements PanacheRepositoryBase<AccessLog, Integer> {

    /**
     * Deletes an AccessLog by id.
     * NOTE: Deleting AccessLogs that are linked to Alerts
     * may violate referential integrity.
     */
    @Transactional
    public void delete(Integer id) {
        AccessLog log = findById(id);
        if (log != null) {
            delete(log);
        }
    }

    /**
     * Fetch a single AccessLog with its AccessCard and Checkpoint eagerly loaded.
     */
    public AccessLog fetchWithCardAndCheckpoint(Integer logId) {

        Query query = getEntityManager().createQuery(
                " select l from AccessLog l " +
                        " join fetch l.accessCard " +
                        " join fetch l.checkpoint " +
                        " where l.logId = :id"
        );
        query.setParameter("id", logId);

        return (AccessLog) query.getSingleResult();
    }

    /**
     * Fetch all AccessLogs for a specific AccessCard.
     */
    public List<AccessLog> fetchByAccessCard(Integer cardId) {

        return find(
                " select l from AccessLog l " +
                        " join fetch l.accessCard c " +
                        " join fetch l.checkpoint " +
                        " where c.cardId = ?1",
                cardId
        ).list();
    }

    /**
     * Fetch all AccessLogs for a specific Checkpoint.
     */
    public List<AccessLog> fetchByCheckpoint(Integer checkpointId) {

        return find(
                " select l from AccessLog l " +
                        " join fetch l.accessCard " +
                        " join fetch l.checkpoint cp " +
                        " where cp.checkpointId = ?1",
                checkpointId
        ).list();
    }

    /**
     * Fetch all denied access attempts.
     */
    public List<AccessLog> fetchDeniedAccesses() {

        return find(
                " select l from AccessLog l " +
                        " join fetch l.accessCard " +
                        " join fetch l.checkpoint " +
                        " where l.accessGranted = ?1",
                PermissionType.AccessDenied
        ).list();
    }

    /**
     * Fetch AccessLogs between two timestamps.
     */
    public List<AccessLog> fetchBetweenDates(Date from, Date to) {

        return find(
                " select l from AccessLog l " +
                        " join fetch l.accessCard " +
                        " join fetch l.checkpoint " +
                        " where l.timestamp between ?1 and ?2",
                from, to
        ).list();
    }

    /**
     * Fetch the most recent N AccessLogs ordered by timestamp.
     */
    public List<AccessLog> fetchLastN(int n) {

        return find(
                " select l from AccessLog l " +
                        " join fetch l.accessCard " +
                        " join fetch l.checkpoint " +
                        " order by l.timestamp desc"
        ).page(0, n).list();
    }
}
