package org.aueb.representation;

import org.aueb.domain.User;
import org.aueb.representation.UserRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "jakarta")
public interface UserMapper {

    // Entity -> Representation
    @Mapping(source = "userId", target = "id")
    UserRepresentation toRepresentation(User user);

    // Representation -> Entity
    @Mapping(target = "userId", ignore = true)            // Το διαχειρίζεται η βάση
    @Mapping(target = "password", ignore = true)          // Ασφάλεια: δεν το δεχόμαστε από απλό JSON
    @Mapping(target = "accessCard", ignore = true)        // Αποφυγή κύκλου
    @Mapping(target = "registrationRequests", ignore = true) // Αποφυγή κύκλου
    User toModel(UserRepresentation representation);

    List<UserRepresentation> toRepresentationList(List<User> users);
}