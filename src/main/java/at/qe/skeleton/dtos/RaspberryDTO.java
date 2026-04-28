package at.qe.skeleton.dtos;

import at.qe.skeleton.model.DeviceStatus;

import java.util.UUID;

public record RaspberryDTO(
        UUID id,
        String name,
        String ipAddress,
        Integer port,
        DeviceStatus status,
        RoomRaspberry room
) {
}
