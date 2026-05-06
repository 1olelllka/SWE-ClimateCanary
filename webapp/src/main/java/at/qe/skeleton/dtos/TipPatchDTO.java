package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotEmpty;

public record TipPatchDTO(
        @NotEmpty(message = "Message must not be empty") //thought can be null meaning it was not set
        String message
) {
}
