package at.qe.skeleton.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record DepartmentWithRoomsCreateDTO(
        @NotBlank(message = "Name must not be blank.")
        String name,
        @NotNull(message = "Building ID must not be blank.")
        UUID buildingID,
        List<UUID> existingRoomIds,
        @Valid
        List<NewRoomInDepartmentDTO> newRooms
) {
}
