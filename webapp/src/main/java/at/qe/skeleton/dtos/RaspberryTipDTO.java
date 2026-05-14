package at.qe.skeleton.dtos;

import java.util.UUID;

public record RaspberryTipDTO(
    String message,
    UUID writeId,
    String deviceName
) {
}
