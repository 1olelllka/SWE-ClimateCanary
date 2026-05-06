package at.qe.skeleton.dtos;

import at.qe.skeleton.model.RoomType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record NewRoomInDepartmentDTO(
        @NotEmpty(message = "Room name must not be empty.")
        String name,
        @NotNull(message = "Room type must not be null.")
        RoomType roomType,
        @NotNull(message = "Default people count must not be null.")
        @Min(value = 1, message = "People count must not be less than one.")
        Integer defaultPeopleCount
) {
}
