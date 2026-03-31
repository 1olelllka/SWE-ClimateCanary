package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.RoomDTO;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper implements DTOMapper<Room, RoomDTO> {
    @Override
    public RoomDTO mapTo(Room entity) {
        return new RoomDTO(entity.getId(), entity.getDepartment().getId(), entity.getDepartment().getName(), entity.getIsActive(),
                entity.getRoomType(), entity.getDefaultPeopleCnt());
    }

    @Override
    public Room mapFrom(RoomDTO dto) {
        return Room.builder()
                .id(dto.id())
                .roomType(dto.roomType())
                .isActive(dto.isActive())
                .defaultPeopleCnt(dto.defaultPeopleCount())
                .department(Department.builder().id(dto.departmentID()).build())
                .build();
    }
}
