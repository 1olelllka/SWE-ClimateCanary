package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record LoginRequestDTO(
        @NotNull(message = "Username must not be null.")
        @NotEmpty(message = "Username must not be empty.")
        String username,
        @NotNull(message = "Password must not be null.")
        @NotEmpty(message = "Password must not be empty.")
        String password
) {
}
