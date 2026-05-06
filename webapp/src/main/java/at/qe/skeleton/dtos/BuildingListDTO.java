package at.qe.skeleton.dtos;

import java.util.UUID;

public record BuildingListDTO(
        UUID id,
        String name,
        String address
) {
}
