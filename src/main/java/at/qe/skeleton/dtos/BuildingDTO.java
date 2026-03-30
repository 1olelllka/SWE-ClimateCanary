package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record BuildingDTO(
        UUID id,
        String address,
        @NotBlank(message = "Name cannot be blank.")
        String name,
        List<BuildingDepartmentsDTO> departments
) {
}
