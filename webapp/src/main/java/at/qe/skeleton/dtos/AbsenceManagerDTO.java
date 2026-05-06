package at.qe.skeleton.dtos;

import java.util.UUID;

public record AbsenceManagerDTO(
        UUID id,
        String firstName,
        String lastName,
        String username
) {
}