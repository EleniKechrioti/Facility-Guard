package org.aueb.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.aueb.domain.AccessLog;
import org.aueb.persistence.AccessLogRepository;
import org.aueb.representation.AccessLogMapper;
import org.aueb.representation.AccessLogRepresentation;

import java.util.Date;
import java.util.List;

@Path("/access-logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccessLogResource {

    @Inject
    AccessLogRepository logRepo;

    @Inject
    AccessLogMapper mapper;

    /* =====================================================
       GET ALL
       ===================================================== */
    @GET
    @Transactional
    public List<AccessLogRepresentation> getAll() {
        return logRepo.fetchAll()
                .stream()
                .map(mapper::toRepresentation)
                .toList();
    }

    /* =====================================================
       GET BY ID
       ===================================================== */
    @GET
    @Path("/{id}")
    @Transactional
    public AccessLogRepresentation getById(@PathParam("id") Integer id) {

        AccessLog log = logRepo.findById(id);
        if (log == null) {
            throw new NotFoundException();
        }

        return mapper.toRepresentation(
                logRepo.fetchWithCardAndCheckpoint(id)
        );
    }

    /* =====================================================
       GET BY CARD
       ===================================================== */
    @GET
    @Path("/card/{cardId}")
    @Transactional
    public List<AccessLogRepresentation> getByCard(
            @PathParam("cardId") Integer cardId
    ) {
        return logRepo.fetchByAccessCard(cardId)
                .stream()
                .map(mapper::toRepresentation)
                .toList();
    }

    /* =====================================================
       GET BY CHECKPOINT
       ===================================================== */
    @GET
    @Path("/checkpoint/{checkpointId}")
    @Transactional
    public List<AccessLogRepresentation> getByCheckpoint(
            @PathParam("checkpointId") Integer checkpointId
    ) {
        return logRepo.fetchByCheckpoint(checkpointId)
                .stream()
                .map(mapper::toRepresentation)
                .toList();
    }

    /* =====================================================
       GET DENIED
       ===================================================== */
    @GET
    @Path("/denied")
    @Transactional
    public List<AccessLogRepresentation> getDenied() {
        return logRepo.fetchDeniedAccesses()
                .stream()
                .map(mapper::toRepresentation)
                .toList();
    }

    /* =====================================================
       GET LAST N
       ===================================================== */
    @GET
    @Path("/last/{n}")
    @Transactional
    public List<AccessLogRepresentation> getLastN(
            @PathParam("n") int n
    ) {
        return logRepo.fetchLastN(n)
                .stream()
                .map(mapper::toRepresentation)
                .toList();
    }

    /* =====================================================
       GET BETWEEN DATES
       ===================================================== */
    @GET
    @Path("/between")
    @Transactional
    public List<AccessLogRepresentation> getBetween(
            @QueryParam("from") long from,
            @QueryParam("to") long to
    ) {
        return logRepo.fetchBetween(
                        new Date(from),
                        new Date(to)
                ).stream()
                .map(mapper::toRepresentation)
                .toList();
    }

    /* =====================================================
       DELETE
       ===================================================== */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Integer id) {

        AccessLog log = logRepo.findById(id);
        if (log == null) {
            throw new NotFoundException();
        }

        logRepo.delete(id);
        return Response.noContent().build(); // 204
    }
}
