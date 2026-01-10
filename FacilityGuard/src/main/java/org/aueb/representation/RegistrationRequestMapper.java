package org.aueb.representation;

import org.aueb.domain.RegistrationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "jakarta", uses = {UserMapper.class})
public interface RegistrationRequestMapper {

    @Mapping(source = "registrationId", target = "id")
    RegistrationRequestRepresentation toRepresentation(RegistrationRequest request);

    @Mapping(target = "registrationId", ignore = true)
    @Mapping(target = "approved", ignore = true) // Το MapStruct δεν μπορεί να το θέσει, άρα το αγνοούμε
    @Mapping(target = "user", ignore = true)     // Δεν θέτουμε ολόκληρο User από το Request DTO
    RegistrationRequest toModel(RegistrationRequestRepresentation representation);

    List<RegistrationRequestRepresentation> toRepresentationList(List<RegistrationRequest> requests);
}