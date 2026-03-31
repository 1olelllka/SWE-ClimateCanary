package at.qe.skeleton.dtos;

import at.qe.skeleton.model.RoomType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RoomCreateDTO(
        @NotNull(message = "Department ID must not be null.")
        UUID departmentID,
        @NotNull(message = "Room type must not be null.")
        RoomType roomType,
        boolean isActive,
        @NotNull(message = "Default people count must not be null.")
        @Min(value = 1, message = "People count must not be less than one.")
        Integer defaultPeopleCount
) {
}
