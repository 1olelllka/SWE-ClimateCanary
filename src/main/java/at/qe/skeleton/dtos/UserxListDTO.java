package at.qe.skeleton.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserxListDTO(
        UUID id,
        LocalDateTime createDate,
        String username,
        String firstName,
        String lastName
) {
}
