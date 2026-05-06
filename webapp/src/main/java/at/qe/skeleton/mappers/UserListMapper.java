package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.UserxListDTO;
import at.qe.skeleton.model.Userx;
import org.springframework.stereotype.Component;

@Component
public class UserListMapper implements DTOMapper<Userx, UserxListDTO> {
    @Override
    public UserxListDTO mapTo(Userx entity) {
        return new UserxListDTO(entity.getId(), entity.getCreateDate(), entity.getUsername(), entity.getFirstName(), entity.getLastName());
    }

    @Override
    public Userx mapFrom(UserxListDTO dto) {
        return Userx.builder()
                .id(dto.id())
                .createDate(dto.createDate())
                .username(dto.username())
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .build();
    }
}
