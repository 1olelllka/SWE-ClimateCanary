package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.BuildingDTO;
import at.qe.skeleton.dtos.BuildingDepartmentsDTO;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BuildingMapper implements DTOMapper<Building, BuildingDTO> {
    @Override
    public BuildingDTO mapTo(Building entity) {
        return new BuildingDTO(entity.getId(), entity.getAddress(), entity.getName(),
                entity.getDepartments() != null ? entity.getDepartments().stream()
                        .map(e -> new BuildingDepartmentsDTO(e.getId(), e.getName())).toList()
                : List.of());
    }

    @Override
    public Building mapFrom(BuildingDTO dto) {
        return Building.builder()
                .id(dto.id())
                .address(dto.address())
                .name(dto.name())
                .departments(dto.departments() != null ? dto.departments().stream()
                        .map(e -> Department.builder().id(dto.id()).build()).toList()
                        : List.of())
                .build();
    }
}
