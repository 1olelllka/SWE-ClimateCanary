package at.qe.skeleton.dtos;

import java.util.List;
import java.util.UUID;

public record BuildingDTO(
        UUID id,
        String address,
        String name,
        List<BuildingDepartmentsDTO> departments
) {
}
