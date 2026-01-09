package org.aueb.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.RequestScoped;
import org.aueb.domain.Building;

import java.util.List;

@RequestScoped
public class BuildingRepository implements PanacheRepositoryBase<Building, Integer> {

    // Find me a building with a specific name (useful for validation)
    public Building findByName(String name) {
        return find("name", name).firstResult();
    }

    // Search by part of the name (case insensitive)
    public List<Building> search(String query) {
        // ?1 corresponds to the query parameter.
        // lower() makes everything small so that uppercase/small doesn't matter.
        return find("lower(name) like lower(?1)", "%" + query + "%").list();
    }
}