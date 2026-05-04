package at.qe.skeleton.services;

import at.qe.skeleton.model.Room;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.RoleRepository;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.repositories.UserxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserSeederService {

    private final UserxRepository userxRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomRepository roomRepository;

    @Value("${app.seeder.default-password}")
    private String defaultSeedPassword;

    public void seed() {
        createUserIfNotExists(
                "employee",
                "Emma",
                "Employee",
                List.of("EMPLOYEE"),
                "ENG-101"
        );

        createUserIfNotExists(
                "depthead",
                "David",
                "Depthead",
                List.of("DEPARTMENT_MANAGER"),
                "ENG-102"
        );

        createUserIfNotExists(
                "senior",
                "Sarah",
                "Senior",
                List.of("HIGHER_MANAGER"),
                "ENG-103"
        );

        createUserIfNotExists(
                "building",
                "Bob",
                "Building",
                List.of("BUILDING_MANAGER"),
                "ENG-101"
        );

        createUserIfNotExists(
                "sysadmin",
                "Sybil",
                "Sysadmin",
                List.of("SYSADMIN"),
                null
        );

        createUserIfNotExists(
                "raspberry-pi",
                "Raspberry",
                "Pi",
                List.of("RASPBERRY_PI"),
                null
        );
    }

    private void createUserIfNotExists(String username,
                                       String firstName,
                                       String lastName,
                                       List<String> roleNames,
                                       String roomNumber) {

        if (userxRepository.findFirstByUsername(username).isPresent()) {
            return;
        }

        Userx user = new Userx();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(defaultSeedPassword));

        Set<UserRole> roles = roleNames.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                .collect(Collectors.toSet());

        user.setUserRoles(roles);

        if (roomNumber != null) {
            Room room = roomRepository.findByRoomNumber(roomNumber)
                    .orElseThrow(() -> new RuntimeException("Room not found: " + roomNumber));

            user.setMyRoom(room);
        }

        userxRepository.save(user);
    }
}