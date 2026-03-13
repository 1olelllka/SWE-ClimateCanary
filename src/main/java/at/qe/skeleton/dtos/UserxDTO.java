package at.qe.skeleton.dtos;

import at.qe.skeleton.model.UserxRole;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Data transfer object for the UserxTypes Entity.
 * 
 * This class is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
public record UserxDTO (
    Long id,
    Long createdBy,
    LocalDateTime createDate,
    Long updatedBy,
    LocalDateTime updateDate,
    String username,
    String firstName,
    String lastName,
    String email,
    String phone,
    boolean enabled,
    @ArraySchema(schema = @Schema(implementation = UserxRole.class))
    Set<UserxRole> roles
) {}
