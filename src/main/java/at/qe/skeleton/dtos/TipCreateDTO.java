package at.qe.skeleton.dtos;

import at.qe.skeleton.model.ViolatedSensor;
import at.qe.skeleton.model.ViolationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TipCreateDTO(

        @NotNull(message = "Room ID must not be null.")
        @NotEmpty(message = "Room ID must not be empty.")
        UUID roomID,

        @NotNull(message = "Violation type must not be null.")
        ViolationType violationType,

        @NotNull(message = "Violation sensor must not be null.")
        ViolatedSensor violatedSensor,

        @NotEmpty(message = "Message must not be empty") //thought can be null meaning it was not set
        String message
) {
}
