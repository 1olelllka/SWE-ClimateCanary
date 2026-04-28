package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

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
    @NotNull
    Set<UUID> roles,
    UUID roomId
) {}
