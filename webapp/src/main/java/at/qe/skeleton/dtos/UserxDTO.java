package at.qe.skeleton.dtos;

import at.qe.skeleton.model.UserRole;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserxDTO (
    UUID id,
    LocalDateTime createDate,
    LocalDateTime updateDate,
    String username,
    String firstName,
    String lastName,
    boolean enabled,
    @ArraySchema(schema = @Schema(implementation = UserRole.class))
    Set<UserRoleDTO> roles,
    UserRoom myRoom,
    Integer numberOfAbsences
) {}
