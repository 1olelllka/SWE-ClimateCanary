package at.qe.skeleton.dtos;

import at.qe.skeleton.model.RoomType;

import java.util.UUID;

public record UserRoom(
        UUID id,
        UUID departmentID,
        String departmentName,
        RoomType roomType
) {
}
