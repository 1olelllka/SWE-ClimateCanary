package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RaspberryCreateDTO(

        @NotBlank(message = "Name must not be blank.")
        String name,

        @NotBlank(message = "IP address must not be blank.")
        String ipAddress,

        @NotNull(message = "Room ID must not be null.")
        UUID roomId
) {
}
