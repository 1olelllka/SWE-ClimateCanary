package at.qe.skeleton.dtos;

import java.util.Set;
import java.util.UUID;

public record UserxPatchDTO(
        String username,
        String firstName,
        String lastName,
        Boolean isEnabled,
        Set<UUID> roles
) {
}
