package org.aueb.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.aueb.domain.AccessLog;
import org.aueb.util.enumerations.PermissionType;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RequestScoped
public class AccessLogRepository
        implements PanacheRepositoryBase<AccessLog, Integer> {

    /* ================= DELETE ================= */

    @Transactional
    public void delete(Integer id) {
        AccessLog log = findById(id);
        if (log != null) {
            delete(log);
        }
    }

    /* ================= FETCH SINGLE ================= */

    public AccessLog fetchWithCardAndCheckpoint(Integer logId) {
        Query query = getEntityManager().createQuery(
                "select l from AccessLog l " +
                        "join fetch l.accessCard " +
                        "join fetch l.checkpoint " +
                        "where l.logId = :id"
        );
        query.setParameter("id", logId);
        return (AccessLog) query.getSingleResult();
    }

    /* ================= FETCH ALL ================= */

    public List<AccessLog> fetchAll() {
        return find(
                "select l from AccessLog l " +
                        "join fetch l.accessCard " +
                        "join fetch l.checkpoint"
        ).list();
    }

    /* ================= BY CARD ================= */

    public List<AccessLog> fetchByAccessCard(Integer cardId) {
        return find(
                "select l from AccessLog l " +
                        "join fetch l.accessCard c " +
                        "join fetch l.checkpoint " +
                        "where c.cardId = ?1",
                cardId
        ).list();
    }

    /* ================= BY CHECKPOINT ================= */

    public List<AccessLog> fetchByCheckpoint(Integer checkpointId) {
        return find(
                "select l from AccessLog l " +
                        "join fetch l.accessCard " +
                        "join fetch l.checkpoint cp " +
                        "where cp.checkpointId = ?1",
                checkpointId
        ).list();
    }

    /* ================= DENIED ================= */

    public List<AccessLog> fetchDeniedAccesses() {
        return find(
                "select l from AccessLog l " +
                        "join fetch l.accessCard " +
                        "join fetch l.checkpoint " +
                        "where l.accessGranted = ?1",
                PermissionType.AccessDenied
        ).list();
    }

    /* ================= BETWEEN DATES ================= */

    public List<AccessLog> fetchBetween(Date from, Date to) {
        return find(
                "select l from AccessLog l " +
                        "join fetch l.accessCard " +
                        "join fetch l.checkpoint " +
                        "where l.timestamp between ?1 and ?2",
                from, to
        ).list();
    }

    /* ================= LAST N ================= */

    public List<AccessLog> fetchLastN(int n) {
        return find(
                "select l from AccessLog l " +
                        "join fetch l.accessCard " +
                        "join fetch l.checkpoint " +
                        "order by l.timestamp desc"
        ).page(0, n).list();
    }

    /* ================= ANTI-PASSBACK ================= */

    public Optional<AccessLog> findLastSuccessfulLog(Integer cardId) {
        return find(
                "accessCard.cardId = ?1 and accessGranted = ?2 order by timestamp desc",
                cardId, PermissionType.AccessGranted
        ).firstResultOptional();
    }
}
