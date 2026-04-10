package at.qe.skeleton.services;

import at.qe.skeleton.model.Permission;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class RoleSeeder implements ApplicationListener<ContextRefreshedEvent> {

    private RoleRepository roleRepository;

    @Autowired
    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.loadRoles();
    }

    @Transactional
    private void loadRoles() {
        String[] names = {"EMPLOYEE", "SYSADMIN", "HIGHER_MANAGER", "DEPARTMENT_MANAGER", "BUILDING_MANAGER"};
        Map<String, Set<Permission>> rolePermissions = Map.of(
                "EMPLOYEE", Set.of(Permission.CAN_VIEW_OWN_OFFICE_CLIMATE, Permission.CAN_VIEW_OWN_SHARED_CLIMATE, Permission.CAN_MANAGE_OWN_ABSENCE),
                "SYSADMIN", Set.of(Permission.CAN_MANAGE_USERS, Permission.CAN_MANAGE_BUILDING_STRUCTURE, Permission.CAN_MANAGE_DEVICES),
                "HIGHER_MANAGER", Set.of(Permission.CAN_VIEW_COMPANY_AGGR, Permission.CAN_VIEW_VIOLATIONS_PER_DEPARTMENT),
                "DEPARTMENT_MANAGER", Set.of(Permission.CAN_VIEW_OWN_DEPARTMENT_LIMITS_VIOLATION, Permission.CAN_VIEW_OWN_DEPARTMENT_MEASURES, Permission.CAN_VIEW_ABSENCE_VIEW, Permission.CAN_MANAGE_ABSENCES),
                "BUILDING_MANAGER", Set.of(Permission.CAN_VIEW_ALL_ROOMS, Permission.CAN_CHANGE_LIMITS, Permission.CAN_MANAGE_TIPS)
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
