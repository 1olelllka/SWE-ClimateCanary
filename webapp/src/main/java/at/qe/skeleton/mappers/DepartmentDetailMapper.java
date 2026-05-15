package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.DepartmentDTO;
import at.qe.skeleton.dtos.UserRoom;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class DepartmentDetailMapper implements DTOMapper<Department, DepartmentDTO> {
    @Override
    public DepartmentDTO mapTo(Department entity) {
        return new DepartmentDTO(entity.getId(), entity.getName(), entity.getBuilding().getId().toString(),
                entity.getBuilding().getName(), entity.getRooms() != null ? entity.getRooms()
                .stream().map(r -> new UserRoom(r.getId(), entity.getId(), entity.getName(), r.getRoomType(), r.getRoomNumber())).toList() : List.of());
    }

    @Override
    public Department mapFrom(DepartmentDTO dto) {
        return Department
                .builder()
                .id(dto.id())
                .name(dto.name())
                .building(Building.builder().id(UUID.fromString(dto.buildingID())).build())
                .rooms(dto.rooms() != null ? dto.rooms().stream().map(room -> Room.builder().id(room.id()).roomNumber(room.roomNumber()).roomType(room.roomType()).build()).toList() : List.of())
                .build();
    }
}
