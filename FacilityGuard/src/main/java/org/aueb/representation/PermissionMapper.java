package org.aueb.representation;

import org.aueb.domain.Permission;
import org.aueb.representation.PermissionRepresentation;
import org.aueb.representation.AreaMapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta", uses = {AreaMapper.class})
public interface PermissionMapper {

    // Entity -> Representation
    // Το MapStruct θα βρει αυτόματα τη μέθοδο toRepresentation του AreaMapper
    // και θα τη χρησιμοποιήσει για να γεμίσει το πεδίο 'area'.
    PermissionRepresentation toRepresentation(Permission entity);

    // Representation -> Entity
    // Αγνοούμε την accessCard (την ορίζουμε από το URL /cards/{id}/...)
    // Το MapStruct θα προσπαθήσει να κάνει map το AreaRepresentation σε Area χρησιμοποιώντας τον AreaMapper.
    @Mapping(target = "accessCard", ignore = true)
    Permission toModel(PermissionRepresentation representation);
}
