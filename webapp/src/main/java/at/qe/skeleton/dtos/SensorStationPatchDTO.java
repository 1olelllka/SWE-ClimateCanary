package at.qe.skeleton.dtos;

import at.qe.skeleton.model.DeviceStatus;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.UUID;

public record SensorStationPatchDTO(
        @Pattern(regexp = "^(?!\\s*$).+", message = "Name must not be blank.")
        String name,
        @PastOrPresent(message = "Last heart beat cannot be in the future.")
        LocalDateTime lastHeartBeat,
        DeviceStatus status,
        UUID roomId
) {
}
