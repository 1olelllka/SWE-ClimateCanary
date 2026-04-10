package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.UserxCreateDTO;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.RoleRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Mapping between UserxCreateDTO and Userx.
 *
 * This class is part of the skeleton project provided for students of the
* course "Software Engineering" offered by Innsbruck University.
 */
@Service
public class UserxCreateMapper implements DTOMapper<Userx, UserxCreateDTO> {

    private RoleRepository roleRepository;

    @Autowired
    public UserxCreateMapper(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public UserxCreateDTO mapTo(Userx entity) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Userx mapFrom(UserxCreateDTO dto) {
        Userx user = new Userx();
        user.setUsername(dto.username());
        user.setPassword(dto.password());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setEnabled(dto.enabled());
        user.setUserRoles(dto.roles() != null ? dto.roles().stream().map(roleRepository::getReferenceById).collect(Collectors.toSet()) : null);
        
        return user;
    }
    
}
