package at.qe.skeleton.dtos;

import at.qe.skeleton.model.Permission;

import java.util.Set;

public record UserRoleCreateDTO(
        String name,
        Set<Permission> permissions
) {
}
