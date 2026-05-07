package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotBlank;

public record TipPatchDTO(
        @NotBlank(message = "Message must not be empty") //thought can be null meaning it was not set
        String message
) {
}
