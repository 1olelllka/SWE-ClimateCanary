package at.qe.skeleton.services;

import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.RoleRepository;
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

    @Value("${app.seeder.default-password}")
    private String defaultSeedPassword;

    public void seed() {

        createUserIfNotExists(
                "employee",
                "Emma",
                "Employee",
                List.of("EMPLOYEE")
        );

        createUserIfNotExists(
                "depthead",
                "David",
                "Depthead",
                List.of("DEPARTMENT_MANAGER")
        );

        createUserIfNotExists(
                "senior",
                "Sarah",
                "Senior",
                List.of("HIGHER_MANAGER")
        );

        createUserIfNotExists(
                "building",
                "Bob",
                "Building",
                List.of("BUILDING_MANAGER")
        );

        createUserIfNotExists(
                "sysadmin",
                "Sybil",
                "Sysadmin",
                List.of("SYSADMIN")
        );

    }

    private void createUserIfNotExists(String username,
                                       String firstName,
                                       String lastName,
                                       List<String> roleNames) {

        if (userxRepository.findFirstByUsername(username).isPresent()) {
            return;
        }

        Userx user = new Userx();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);

        user.setPassword(passwordEncoder.encode(defaultSeedPassword));

        // attach roles
        Set<UserRole> roles = roleNames.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                .collect(Collectors.toSet());

        user.setUserRoles(roles);

        userxRepository.save(user);
    }
}
