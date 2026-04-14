package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.UserRoom;
import at.qe.skeleton.dtos.UserxDTO;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.model.Userx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class UserxMapper implements DTOMapper<Userx, UserxDTO>{

    private final UserRoleMapper roleMapper;

    @Autowired
    public UserxMapper(UserRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

   @Override
    public UserxDTO mapTo(Userx user) {
        if (user == null) {
            return null;
        }

       return new UserxDTO(
               user.getId(),
               user.getCreateDate(),
               user.getUpdateDate(),
               user.getUsername(),
               user.getFirstName(),
               user.getLastName(),
               user.getEnabled(),
               user.getSnoozedWarningsUntil(),
               user.getUserRoles().stream().map(roleMapper::mapTo).collect(Collectors.toSet()),
               user.getMyRoom() != null
               ?
               new UserRoom(user.getMyRoom().getId(), user.getMyRoom().getDepartment().getId(),
                       user.getMyRoom().getDepartment().getName(), user.getMyRoom().getRoomType())
               : null
       );
    }

    @Override
    public Userx mapFrom(UserxDTO userxDto) {
        return Userx.builder()
                .id(userxDto.id())
                .firstName(userxDto.firstName())
                .lastName(userxDto.lastName())
                .enabled(userxDto.enabled())
                .userRoles(userxDto.roles().stream().map(roleMapper::mapFrom).collect(Collectors.toSet()))
                .myRoom(userxDto.myRoom() != null ? Room.builder().id(userxDto.myRoom().id()).build() : null)
                .build();
    }
}