package at.qe.skeleton.dtos;

import at.qe.skeleton.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * Reduced data transfer object for the UserxTypes Entity in the create endpoint.
 *
 * This class is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
public record UserxCreateDTO(
    @NotBlank
    String username,
    @NotBlank
    String password,
    String firstName,
    String lastName,
    boolean enabled,
    @ArraySchema(schema = @Schema(implementation = UserRole.class))
    Set<UserRole> roles
) {}
