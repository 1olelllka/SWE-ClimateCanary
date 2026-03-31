package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.DepartmentListDTO;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DepartmentListMapper implements DTOMapper<Department, DepartmentListDTO> {
    @Override
    public DepartmentListDTO mapTo(Department entity) {
        return new DepartmentListDTO(entity.getId(),
                entity.getName(),
                entity.getBuilding().getId().toString(),
                entity.getBuilding().getName());
    }

    @Override
    public Department mapFrom(DepartmentListDTO dto) {
        return Department
                .builder()
                .id(dto.id())
                .name(dto.name())
                .building(Building.builder().id(UUID.fromString(dto.buildingID())).build())
                .build();
    }
}
