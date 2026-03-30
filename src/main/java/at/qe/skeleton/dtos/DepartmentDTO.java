package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record DepartmentDTO(
        UUID id,
        @NotBlank(message = "Name must not be blank.")
        String name,
        @NotBlank(message = "Building ID must not be blank.")
        String buildingID,
        String buildingName,
        List<UUID> roomNumbers
) {
}
