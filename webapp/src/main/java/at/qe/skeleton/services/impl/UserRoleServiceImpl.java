package at.qe.skeleton.services.impl;

import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.repositories.RoleRepository;
import at.qe.skeleton.services.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for managing {@link UserRole} entities.
 * <p>
 * Provides operations to retrieve and update user roles (permissions)
 * using the underlying {@link RoleRepository}.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final RoleRepository roleRepository;

    /**
     * Retrieves all available user roles (permissions) from the database.
     *
     * @return a list of {@link UserRole} objects representing all roles
     */
    @Override
    public List<UserRole> getListOfPermissions() {
        return roleRepository.findAll();
    }

    /**
     * Updates an existing user role with the provided data.
     * <p>
     * Only non-null fields from the provided {@code dto} are applied to the existing entity.
     * If the role with the given ID does not exist, a {@link NotFoundException} is thrown.
     * </p>
     *
     * @param id  the unique identifier of the role to update
     * @param dto the role data containing updated values
     * @return the updated and persisted {@link UserRole} entity
     * @throws NotFoundException if no role exists with the given {@code id}
     */
    @Override
    public UserRole updateExistingPermission(UUID id, UserRole dto) {
        return roleRepository.findById(id).map(role -> {
            Optional.ofNullable(dto.getName()).ifPresent(role::setName);
            Optional.ofNullable(dto.getPermissions()).ifPresent(role::setPermissions);
            log.info("Updated role {}", role.getName());
            return roleRepository.save(role);
        }).orElseThrow(() -> new NotFoundException("Permission with id " + id + " does not exist."));
    }
}