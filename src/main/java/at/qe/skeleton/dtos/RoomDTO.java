package at.qe.skeleton.dtos;

import at.qe.skeleton.model.RoomType;

import java.util.UUID;

public record RoomDTO(
        UUID id,
        UUID departmentID,
        RoomType roomType,
        Integer defaultPeopleCount
) {
}
