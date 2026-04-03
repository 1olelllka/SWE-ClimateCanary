package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.UserxCreateDTO;
import at.qe.skeleton.model.Userx;
import org.springframework.stereotype.Service;

/**
 * Mapping between UserxCreateDTO and Userx.
 *
 * This class is part of the skeleton project provided for students of the
* course "Software Engineering" offered by Innsbruck University.
 */
@Service
public class UserxCreateMapper implements DTOMapper<Userx, UserxCreateDTO> {

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
        
        return user;
    }
    
}
