package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.BuildingListDTO;
import at.qe.skeleton.model.Building;
import org.springframework.stereotype.Component;

@Component
public class BuildingListMapper implements DTOMapper<Building, BuildingListDTO> {
    @Override
    public BuildingListDTO mapTo(Building entity) {
        return new BuildingListDTO(entity.getId(), entity.getName(), entity.getAddress());
    }

    @Override
    public Building mapFrom(BuildingListDTO dto) {
        return Building.builder()
                .id(dto.id())
                .address(dto.address())
                .name(dto.name())
                .build();
    }
}
