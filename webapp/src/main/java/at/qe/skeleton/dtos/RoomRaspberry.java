package at.qe.skeleton.dtos;

import java.util.UUID;

public record RoomRaspberry(
        UUID roomId,
        String roomName
) {
}
