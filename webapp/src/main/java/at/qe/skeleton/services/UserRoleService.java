package at.qe.skeleton.services;

import at.qe.skeleton.model.UserRole;

import java.util.List;
import java.util.UUID;

public interface UserRoleService {
    List<UserRole> getListOfPermissions();
    UserRole updateExistingPermission(UUID id, UserRole dto);
}
