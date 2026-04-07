package at.qe.skeleton.dtos;

import at.qe.skeleton.model.ViolatedSensor;
import at.qe.skeleton.model.ViolationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TipCreateDTO(

        @NotNull(message = "Room ID must not be null.")
        @NotEmpty(message = "Room ID must not be empty.")
        UUID roomID,

        @NotBlank(message = "Violation type must not be blank.")
        ViolationType violationType,

        @NotBlank(message = "Violation sensor must not be blank.")
        ViolatedSensor violatedSensor,

        @NotEmpty(message = "Message must not be empty") //thought can be null meaning it was not set
        String message
) {
}
