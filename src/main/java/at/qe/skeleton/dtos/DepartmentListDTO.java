package at.qe.skeleton.dtos;

import java.util.UUID;

public record DepartmentListDTO(
        UUID id,
        String name,
        String buildingID,
        String buildingName
) {
}
