package org.aueb.representation;

import org.aueb.domain.RegistrationRequest;
import org.aueb.representation.RegistrationRequestRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * uses = {UserMapper.class}: Λέμε στο MapStruct να χρησιμοποιήσει τον
 * UserMapper για να μετατρέψει τον User που βρίσκεται μέσα στο Request.
 */
@Mapper(componentModel = "jakarta", uses = {UserMapper.class})
public interface RegistrationRequestMapper {

    @Mapping(source = "registrationId", target = "id")
    RegistrationRequestRepresentation toRepresentation(RegistrationRequest request);

    @Mapping(target = "registrationId", ignore = true)
    RegistrationRequest toModel(RegistrationRequestRepresentation representation);

    List<RegistrationRequestRepresentation> toRepresentationList(List<RegistrationRequest> requests);
}