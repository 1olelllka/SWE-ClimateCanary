package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotBlank;

public record BuildingCreateDTO(
        @NotBlank(message = "Name cannot be blank.")
        String name,
        String address
) {
}
