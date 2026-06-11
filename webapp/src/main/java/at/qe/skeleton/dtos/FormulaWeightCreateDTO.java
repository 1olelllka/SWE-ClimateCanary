package at.qe.skeleton.dtos;

import jakarta.validation.constraints.NotNull;

public record FormulaWeightCreateDTO(
        @NotNull(message = "Temperature weight must not be null.")
        Double tempWeight,
        @NotNull(message = "CO2 weight must not be null.")
        Double co2Weight,
        @NotNull(message = "Humidity weight must not be null.")
        Double humWeight
) {
}
