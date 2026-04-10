package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.UserxPatchDTO;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserPatchMapper implements DTOMapper<Userx, UserxPatchDTO> {

    private RoleRepository roleRepository;
    @Autowired
    public UserPatchMapper(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public UserxPatchDTO mapTo(Userx entity) {
        return new UserxPatchDTO(entity.getUsername(), entity.getFirstName(), entity.getLastName(), entity.getEnabled(),
                entity.getUserRoles() != null ? entity.getUserRoles().stream().map(UserRole::getId).collect(Collectors.toSet())
                : null);
    }

    @Override
    public Userx mapFrom(UserxPatchDTO dto) {
        return Userx.builder()
                .username(dto.username())
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .enabled(dto.isEnabled())
                .userRoles(dto.roles() != null
                ? dto.roles().stream().map(uuid -> roleRepository.getReferenceById(uuid)).collect(Collectors.toSet())
                : null)
                .build();
    }
}
