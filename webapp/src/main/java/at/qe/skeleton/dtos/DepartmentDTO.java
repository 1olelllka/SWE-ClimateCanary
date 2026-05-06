package at.qe.skeleton.dtos;

import java.util.List;
import java.util.UUID;

public record DepartmentDTO(
        UUID id,
        String name,
        String buildingID,
        String buildingName,
        List<UUID> roomNumbers
) {
}
