package at.qe.skeleton.services.impl;

import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.repositories.RoleRepository;
import at.qe.skeleton.services.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserRoleServiceImpl implements UserRoleService {

    private RoleRepository roleRepository;

    @Autowired
    public UserRoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public List<UserRole> getListOfPermissions() {
        return roleRepository.findAll();
    }

    @Override
    public UserRole updateExistingPermission(UUID id, UserRole dto) {
        return roleRepository.findById(id).map(role -> {
            Optional.ofNullable(dto.getName()).ifPresent(role::setName);
            Optional.ofNullable(dto.getPermissions()).ifPresent(role::setPermissions);
            return roleRepository.save(role);
        }).orElseThrow(() -> new NotFoundException("Permission with id " + id + " does not exist."));
    }
}
