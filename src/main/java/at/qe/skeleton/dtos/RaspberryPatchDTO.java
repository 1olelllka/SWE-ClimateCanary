package at.qe.skeleton.dtos;

import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record RaspberryPatchDTO(
        @Pattern(regexp = "^(?!\\s*$).+", message = "Name must not be blank.")
        String name,
        String ipAddress,
        UUID roomId,
        Integer frequency
) {
}
