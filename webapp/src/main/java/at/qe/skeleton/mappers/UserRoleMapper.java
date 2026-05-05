package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.UserRoleDTO;
import at.qe.skeleton.model.UserRole;
import org.springframework.stereotype.Component;

@Component
public class UserRoleMapper implements DTOMapper<UserRole, UserRoleDTO> {
    @Override
    public UserRoleDTO mapTo(UserRole entity) {
        return new UserRoleDTO(entity.getId(), entity.getName(), entity.getPermissions());
    }

    @Override
    public UserRole mapFrom(UserRoleDTO dto) {
        return UserRole.builder()
                .id(dto.id())
                .name(dto.name())
                .permissions(dto.permissions())
                .build();
    }
}
