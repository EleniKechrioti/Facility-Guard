package org.aueb.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.aueb.domain.Checkpoint;

import java.util.List;

@RequestScoped
public class CheckpointRepository
        implements PanacheRepositoryBase<Checkpoint, Integer> {

    /**
     * Fetch a Checkpoint with its Area and Permission eagerly loaded.
     * Useful for REST or service layer to avoid lazy-loading issues.
     */
    public Checkpoint fetchWithAreaAndPermission(Integer checkpointId) {

        Query query = getEntityManager().createQuery(
                " select c from Checkpoint c " +
                        " join fetch c.area " +
                        " left join fetch c.permission " +
                        " where c.checkpointId = :id"
        );
        query.setParameter("id", checkpointId);

        return (Checkpoint) query.getSingleResult();
    }

    /**
     * Fetch all Checkpoints belonging to a specific Area.
     */
    public List<Checkpoint> fetchByArea(Integer areaId) {

        return find(
                " select c from Checkpoint c " +
                        " join fetch c.area a " +
                        " left join fetch c.permission " +
                        " where a.areaId = ?1",
                areaId
        ).list();
    }

    /**
     * Delete a Checkpoint by id.
     * Note: Checkpoints may be referenced by AccessLogs.
     */
    @Transactional
    public void delete(Integer id) {
        Checkpoint checkpoint = findById(id);
        if (checkpoint != null) {
            delete(checkpoint);
        }
    }
}
