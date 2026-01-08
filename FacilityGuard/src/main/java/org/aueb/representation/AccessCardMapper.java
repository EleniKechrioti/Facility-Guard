package org.aueb.representation;

import org.aueb.domain.AccessCard;
import org.aueb.representation.AccessCardRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * uses = {UserMapper.class}: Χρειαζόμαστε τον UserMapper για να μετατρέψει
 * τον User entity σε UserRepresentation (το πεδίο 'holder') και αντίστροφα.
 */
@Mapper(componentModel = "jakarta", uses = {UserMapper.class})
public interface AccessCardMapper {

    // --- Entity -> Representation ---
    @Mapping(source = "cardId", target = "id")
    @Mapping(source = "user", target = "holder") // Λέμε ότι το user του Entity πάει στο holder του DTO
    AccessCardRepresentation toRepresentation(AccessCard card);

    // --- Representation -> Entity ---
    @Mapping(target = "cardId", ignore = true)      // Το ID το διαχειρίζεται η βάση
    @Mapping(source = "holder", target = "user")    // Το holder του DTO γίνεται user στο Entity
    @Mapping(target = "permissions", ignore = true) // Δεν περνάμε permissions μέσω απλού JSON
    @Mapping(target = "accessLogs", ignore = true)  // Δεν περνάμε logs μέσω απλού JSON
    AccessCard toModel(AccessCardRepresentation representation);

    // --- Λίστα -> Λίστα ---
    List<AccessCardRepresentation> toRepresentationList(List<AccessCard> cards);
}