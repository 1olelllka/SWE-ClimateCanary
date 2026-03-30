package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.BuildingDTO;
import at.qe.skeleton.model.Building;
import org.springframework.stereotype.Component;

@Component
public class BuildingMapper implements DTOMapper<Building, BuildingDTO> {
    @Override
    public BuildingDTO mapTo(Building entity) {
        return new BuildingDTO(entity.getId(), entity.getAddress(), entity.getName());
    }

    @Override
    public Building mapFrom(BuildingDTO dto) {
        return Building.builder()
                .id(dto.id())
                .address(dto.address())
                .name(dto.name())
                .build();
    }
}
