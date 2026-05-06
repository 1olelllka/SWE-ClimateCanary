package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.UserxCreateDTO;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.RoleRepository;
import at.qe.skeleton.repositories.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class UserxCreateMapper implements DTOMapper<Userx, UserxCreateDTO> {

    private final RoleRepository roleRepository;
    private final RoomRepository roomRepository; // NEU

    @Autowired
    public UserxCreateMapper(RoleRepository roleRepository, RoomRepository roomRepository) {
        this.roleRepository = roleRepository;
        this.roomRepository = roomRepository; // NEU
    }

    @Override
    public UserxCreateDTO mapTo(Userx entity) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    @Transactional
    public Userx mapFrom(UserxCreateDTO dto) {
        Userx user = new Userx();
        user.setUsername(dto.username());
        user.setPassword(dto.password());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setEnabled(dto.enabled());
        user.setUserRoles(dto.roles().stream()
                .map(roleRepository::getReferenceById)
                .collect(Collectors.toSet()));

        if (dto.roomId() != null) {
            roomRepository.findById(dto.roomId()).ifPresent(user::setMyRoom);
        }

        return user;
    }
}