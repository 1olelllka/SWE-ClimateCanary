package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.UserxDTO;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.UserxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Mapping between Userx and UserxDTOs.
 *
 * This class is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
@Service
public class UserxMapper implements DTOMapper<Userx, UserxDTO>{
    
    private final UserxService userxService;
    
    @Autowired
    public UserxMapper(UserxService userxService) {
        this.userxService = userxService;
    }
    
   @Override
    public UserxDTO mapTo(Userx user) {
        if (user == null) {
            return null;
        }
        UserxDTO dto = new UserxDTO(
                user.getId(), 
                user.getCreateUser().getId(), 
                user.getCreateDate(), 
                user.getUpdateUser() != null ? user.getUpdateUser().getId() : null, 
                user.getUpdateDate(),
                user.getUsername(), 
                user.getFirstName(), 
                user.getLastName(), 
                user.getEmail(), 
                user.getPhone(), 
                user.isEnabled(), 
                user.getRoles()
        );
        
        return dto;
    }

    @Override
    public Userx mapFrom(UserxDTO userxDto) {
        if (null == userxDto) {
            return null;
        }
        Userx user;
        if (null != userxDto.id()) {
            user = userxService.loadUser(userxDto.id()).orElse(new Userx());
        } else {
            user = new Userx();
        }
        user.setFirstName(userxDto.firstName());
        user.setLastName(userxDto.lastName());
        user.setEmail(userxDto.email());
        user.setPhone(userxDto.phone());
        user.setEnabled(userxDto.enabled());
        user.setRoles(userxDto.roles());

        return user;
    }
}