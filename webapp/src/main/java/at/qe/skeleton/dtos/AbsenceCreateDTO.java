package at.qe.skeleton.dtos;

import at.qe.skeleton.model.AbsenceType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record AbsenceCreateDTO(
        @NotNull(message = "User id must not be null.")
        UUID userId,
        @NotNull(message = "Starting date must be not null.")
        LocalDateTime startDate,
        @NotNull(message = "Ending date must be not null.")
        LocalDateTime endDate,
        @NotNull(message = "Absence reason must not be null.")
        AbsenceType reason,
        String comment,
        @NotNull(message = "Manager ID must not be empty.")
        UUID assignedTo
) {
}