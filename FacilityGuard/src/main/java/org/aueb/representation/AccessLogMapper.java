package org.aueb.representation;

import jakarta.inject.Inject;
import org.aueb.domain.AccessCard;
import org.aueb.domain.AccessLog;
import org.aueb.domain.Checkpoint;
import org.aueb.persistence.AccessCardRepository;
import org.aueb.persistence.CheckpointRepository;
import org.mapstruct.*;

@Mapper(
        componentModel = "cdi"
)
public abstract class AccessLogMapper {

    @Inject
    AccessCardRepository accessCardRepository;

    @Inject
    CheckpointRepository checkpointRepository;

    /* ===================== ENTITY → DTO ===================== */

    @Mapping(target = "cardId", source = "accessCard.cardId")
    @Mapping(target = "checkpointId", source = "checkpoint.checkpointId")
    public abstract AccessLogRepresentation toRepresentation(AccessLog entity);

    /* ===================== DTO → ENTITY ===================== */

    @Mapping(target = "accessCard", ignore = true)
    @Mapping(target = "checkpoint", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    public abstract AccessLog toModel(AccessLogRepresentation dto);

    /* ===================== AFTER MAPPING ===================== */

    @AfterMapping
    public void resolveRelations(
            AccessLogRepresentation dto,
            @MappingTarget AccessLog entity
    ) {
        if (dto.cardId != null) {
            AccessCard card = accessCardRepository.findById(dto.cardId);
            entity.setAccessCard(card);
        }

        if (dto.checkpointId != null) {
            Checkpoint checkpoint = checkpointRepository.findById(dto.checkpointId);
            entity.setCheckpoint(checkpoint);
        }
    }
}
