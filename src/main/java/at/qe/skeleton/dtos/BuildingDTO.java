package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record BuildingDTO(
        UUID id,
        String address,
        String name,
        List<BuildingDepartmentsDTO> departments
) {
}
