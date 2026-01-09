package org.aueb.representation;

import org.aueb.domain.Alert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "jakarta")
public interface AlertMapper {

    @Mapping(source = "alertId", target = "id")
    @Mapping(source = "accessLog.logId", target = "accessLogId")
    AlertRepresentation toRepresentation(Alert alert);

    List<AlertRepresentation> toRepresentationList(List<Alert> alerts);
}