package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SensorStationCreateDTO(

        @NotBlank(message = "Sensor station name must not be blank.")
        String name,

        @NotBlank(message = "BLE Hardware Address must not be blank.")
        String bleHardwareAddress,

        @NotNull(message = "Room ID must not be null.")
        @NotEmpty(message = "Room ID must not be empty.")
        UUID roomId,

        @NotNull(message = "Pi ID must not be null.")
        @NotEmpty(message = "Pi ID must not be empty.")
        UUID connectedToPiId
) {
}
