package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.RoomDTO;
import at.qe.skeleton.dtos.UserxListDTO;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.model.Userx;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class RoomMapper implements DTOMapper<Room, RoomDTO> {

    @Override
    public RoomDTO mapTo(Room entity) {
        return new RoomDTO(entity.getId(), entity.getDepartment().getId(), entity.getDepartment().getName(), entity.getIsActive(),
                entity.getRoomType(), entity.getDefaultPeopleCnt(),
                entity.getUsers() != null ?
                        entity.getUsers().stream()
                                .map(u -> new UserxListDTO(u.getId(), u.getCreateDate(), u.getUsername(), u.getFirstName(), u.getLastName())).collect(Collectors.toSet())
                :  null,
                entity.getRoomNumber());
    }

    @Override
    public Room mapFrom(RoomDTO dto) {
        return Room.builder()
                .id(dto.id())
                .roomNumber(dto.name())
                .roomType(dto.roomType())
                .isActive(dto.isActive())
                .defaultPeopleCnt(dto.defaultPeopleCount())
                .department(Department.builder().id(dto.departmentID()).build())
                .users(dto.users().stream().map(u -> Userx.builder().id(u.id()).build()).collect(Collectors.toSet()))
                .build();
    }
}
