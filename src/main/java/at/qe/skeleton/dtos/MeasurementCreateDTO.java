package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MeasurementCreateDTO(

        @NotNull(message = "Room ID must not be null.")
        UUID roomId,


        @NotNull(message = "Timestamp must not be null.")
        LocalDateTime timestamp,

        @NotNull(message = "Readings must not be null.")
        @NotEmpty(message = "Readings must not be empty.")
        List<ReadingDTO> readings
) {
}
