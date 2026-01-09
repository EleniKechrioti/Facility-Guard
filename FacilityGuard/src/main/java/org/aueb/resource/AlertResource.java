package org.aueb.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.aueb.persistence.AlertRepository;
import org.aueb.representation.AlertMapper;
import org.aueb.representation.AlertRepresentation;

import java.util.List;

@Path("/alerts")
@Produces(MediaType.APPLICATION_JSON)
public class AlertResource {

    @Inject
    AlertRepository alertRepository;

    @Inject
    AlertMapper alertMapper;

    /**
     * GET /alerts
     * Returns a list of all alarms.
     */
    @GET
    public List<AlertRepresentation> getAllAlerts() {
        // Χρησιμοποιούμε τη μέθοδο που φτιάξαμε στο Repository για ταξινόμηση
        return alertMapper.toRepresentationList(alertRepository.listAllSorted());
    }
}