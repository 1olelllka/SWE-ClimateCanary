package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DepartmentCreateDTO(
        @NotBlank(message = "Name must not be blank.")
        String name,
        @NotNull(message = "Building ID must not be blank.")
        UUID buildingId
) {
}
