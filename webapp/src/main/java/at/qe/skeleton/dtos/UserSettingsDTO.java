package at.qe.skeleton.dtos;

import at.qe.skeleton.model.DateFormat;

import java.util.UUID;

public record UserSettingsDTO(
        UUID userId,
        boolean darkMode,
        boolean fahrenheit,
        DateFormat format,
        boolean twelveHourFormat
) {
}
