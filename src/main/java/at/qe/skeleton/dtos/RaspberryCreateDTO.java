package at.qe.skeleton.dtos;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record RaspberryCreateDTO(

        @NotBlank(message = "Name must not be blank.")
        String name,

        @NotBlank(message = "IP address must not be blank.")
        String ipAddress,

        @NotNull(message = "Port must not be null.")
        @Min(value = 1000, message = "Port must be minimum 1000.")
        @Max(value = 9999, message = "Port must be maximum 9999.")
        Integer port,

        @NotNull(message = "Room ID must not be null.")
        UUID roomId
) {
}
