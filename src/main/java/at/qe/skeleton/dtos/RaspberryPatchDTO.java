package at.qe.skeleton.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record RaspberryPatchDTO(
        @Pattern(regexp = "^(?!\\s*$).+", message = "Name must not be blank.")
        String name,
        String ipAddress,
        UUID roomId,
        Integer frequency,
        @Min(value = 1000, message = "Port must be minimum 1000.")
        @Max(value = 9999, message = "Port must be maximum 9999.")
        Integer port
) {
}
