package at.qe.skeleton.dtos;

import at.qe.skeleton.model.DeviceStatus;

import java.util.UUID;

public record SensorStationDTO(
        UUID readId,
        UUID writeId,
        String name,
        DeviceStatus status,
        UUID roomId,
        UUID connectedToPiId
) {
}
