package at.qe.skeleton.dtos;

import at.qe.skeleton.model.Permission;

import java.util.Set;
import java.util.UUID;

public record UserRoleDTO(
        UUID id,
        String name,
        Set<Permission> permissions
) {
}
