package at.qe.skeleton.dtos;

import at.qe.skeleton.model.AbsenceStatus;
import at.qe.skeleton.model.AbsenceType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AbsenceDTO(
        UUID id,
        AbsenceType typeOfAbsense,
        AbsenceStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime createdAt,
        UUID assignedTo,
        String comment
) {
}
