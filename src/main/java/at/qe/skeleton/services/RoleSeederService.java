package at.qe.skeleton.services;

import at.qe.skeleton.model.Permission;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleSeederService {

    private final RoleRepository roleRepository;

    @Transactional
    public void loadRoles() {
        String[] names = {"EMPLOYEE", "SYSADMIN", "HIGHER_MANAGER", "DEPARTMENT_MANAGER", "BUILDING_MANAGER", "RASPBERRY_PI"};
        Map<String, Set<Permission>> rolePermissions = Map.of(
                "EMPLOYEE", Set.of(Permission.CAN_VIEW_OWN_OFFICE_CLIMATE, Permission.CAN_VIEW_OWN_SHARED_CLIMATE, Permission.CAN_MANAGE_OWN_ABSENCE),
                "SYSADMIN", Set.of(Permission.CAN_MANAGE_USERS, Permission.CAN_MANAGE_BUILDING_STRUCTURE, Permission.CAN_MANAGE_DEVICES, Permission.CAN_GET_RASPBERRY_CONFIG, Permission.CAN_VIEW_OCCUPANCY),
                "HIGHER_MANAGER", Set.of(Permission.CAN_VIEW_COMPANY_AGGR, Permission.CAN_VIEW_VIOLATIONS_PER_DEPARTMENT),
                "DEPARTMENT_MANAGER", Set.of(Permission.CAN_VIEW_OWN_DEPARTMENT_LIMITS_VIOLATION, Permission.CAN_VIEW_OWN_DEPARTMENT_MEASURES, Permission.CAN_VIEW_ABSENCE_VIEW, Permission.CAN_MANAGE_ABSENCES, Permission.CAN_MANAGE_OWN_ABSENCE),
                "BUILDING_MANAGER", Set.of(Permission.CAN_VIEW_ALL_ROOMS, Permission.CAN_CHANGE_LIMITS, Permission.CAN_MANAGE_TIPS),
                "RASPBERRY_PI", Set.of(Permission.CAN_GET_RASPBERRY_CONFIG, Permission.CAN_VIEW_OCCUPANCY, Permission.CAN_SEND_WARNINGS, Permission.CAN_SEND_MEASUREMENTS)
        );
        Arrays.stream(names).forEach((roleName) -> {
            Optional<UserRole> optionalRole = roleRepository.findByName(roleName);
            optionalRole.ifPresentOrElse(System.out::println, () -> {
                UserRole role = UserRole.builder()
                        .name(roleName)
                        .permissions(rolePermissions.get(roleName))
                        .build();
                roleRepository.save(role);
            });
        });
    }
}
