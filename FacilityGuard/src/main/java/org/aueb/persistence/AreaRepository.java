package org.aueb.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.RequestScoped;
import org.aueb.domain.Area;

import java.util.List;

@RequestScoped
public class AreaRepository implements PanacheRepositoryBase<Area, Integer> {

    // Bring me all the zones of the building with ID = X
    public List<Area> findByBuildingId(Integer buildingId) {
        return find("building.id", buildingId).list();
    }
}