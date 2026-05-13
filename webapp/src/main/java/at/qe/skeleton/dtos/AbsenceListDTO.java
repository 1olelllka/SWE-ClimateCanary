package at.qe.skeleton.dtos;

import at.qe.skeleton.model.AbsenceStatus;
import at.qe.skeleton.model.AbsenceType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AbsenceListDTO(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String roomNumber,
        LocalDateTime startDate,
        LocalDateTime endDate,
        AbsenceType typeOfAbsence,
        AbsenceStatus status,
        LocalDateTime createdAt,
        String comment,
        String managerFirstName,
        String managerLastName
) {
}
