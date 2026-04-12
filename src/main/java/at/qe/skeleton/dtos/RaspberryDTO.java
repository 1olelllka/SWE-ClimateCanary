package at.qe.skeleton.dtos;

import at.qe.skeleton.model.DeviceStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.UUID;

public record RaspberryDTO(
        UUID id,
        String name,
        String ipAddress,
        DeviceStatus status,
        UUID roomId,
        String roomNumber
) {
}
