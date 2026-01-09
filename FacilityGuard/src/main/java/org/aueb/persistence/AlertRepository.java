package org.aueb.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.aueb.domain.Alert;

import java.util.List;

@ApplicationScoped
public class AlertRepository implements PanacheRepositoryBase<Alert, Integer> {

    /**
     * Returns Alerts sorted from most recent to oldest.
     */
    public List<Alert> listAllSorted() {
        return find("order by timestamp desc").list();
    }
}
