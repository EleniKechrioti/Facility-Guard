package org.aueb.representation.util;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SecurityExceptionMapper implements ExceptionMapper<SecurityException> {

    @Override
    public Response toResponse(SecurityException exception) {
        // Returns 403 Forbidden
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
    }
}