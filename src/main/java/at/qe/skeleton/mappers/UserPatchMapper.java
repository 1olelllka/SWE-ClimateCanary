package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.UserxPatchDTO;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.RoleRepository;
import at.qe.skeleton.repositories.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserPatchMapper implements DTOMapper<Userx, UserxPatchDTO> {

    private final RoleRepository roleRepository;
    private final RoomRepository roomRepository;

    @Override
    public UserxPatchDTO mapTo(Userx entity) {
        return new UserxPatchDTO(entity.getUsername(), entity.getFirstName(), entity.getLastName(), entity.getEnabled(),
                entity.getUserRoles() != null ? entity.getUserRoles().stream().map(UserRole::getId).collect(Collectors.toSet())
                : null,
                entity.getMyRoom() != null ? entity.getMyRoom().getId() : null);
    }

    @Override
    public Userx mapFrom(UserxPatchDTO dto) {
        try {
            return Userx.builder()
                    .username(dto.username())
                    .firstName(dto.firstName())
                    .lastName(dto.lastName())
                    .enabled(dto.isEnabled())
                    .userRoles(dto.roles() != null
                            ? dto.roles().stream().map(uuid -> roleRepository.getReferenceById(uuid)).collect(Collectors.toSet())
                            : null)
                    .myRoom(dto.roomId() != null ? roomRepository.findById(dto.roomId()).orElseThrow(() -> new NotFoundException("Room with id " + dto.roomId() + " was not found.")) : null)
                    .build();
        } catch (EntityNotFoundException ex) {
            throw new NotFoundException(ex.getMessage());
        }
    }
}
