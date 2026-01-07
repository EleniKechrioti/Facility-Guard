package org.aueb.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.aueb.domain.RegistrationRequest;
import org.aueb.util.enumerations.ActivityStatus;

import java.util.List;

@ApplicationScoped
public class RegistrationRequestRepository implements PanacheRepository<RegistrationRequest> {

    public List<RegistrationRequest> findPendingRequests() {
        return find("status", ActivityStatus.Active).list();
    }
}