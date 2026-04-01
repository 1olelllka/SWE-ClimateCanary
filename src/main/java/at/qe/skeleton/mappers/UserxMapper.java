package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.UserxDTO;
import at.qe.skeleton.model.Userx;
import org.springframework.stereotype.Service;

@Service
public class UserxMapper implements DTOMapper<Userx, UserxDTO>{
    
//    private final UserxService userxService;
    
//    @Autowired
//    public UserxMapper(UserxService userxService) {
//        this.userxService = userxService;
//    }
    
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
               user.isEnabled(),
               user.getSnoozedWarningsUntil(),
               user.getUserRoles()
       );
    }

    @Override
    public Userx mapFrom(UserxDTO userxDto) {
        return Userx.builder()
                .id(userxDto.id())
                .firstName(userxDto.firstName())
                .lastName(userxDto.lastName())
                .enabled(userxDto.enabled())
                .userRoles(userxDto.roles())
                .build();
    }
}