package at.qe.skeleton.dtos;

import at.qe.skeleton.model.ViolatedSensor;
import at.qe.skeleton.model.ViolationType;
import at.qe.skeleton.model.WarningStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record TipCreateDTO(

        @NotNull(message = "Violation type must not be null.")
        ViolationType violationType,

        @NotNull(message = "Violation sensor must not be null.")
        ViolatedSensor violatedSensor,

        @NotNull(message = "Violation status must not be null.")
        WarningStatus violationStatus,

        @NotEmpty(message = "Message must not be empty") //thought can be null meaning it was not set
        String message
) {
}
