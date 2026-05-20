package at.qe.skeleton.dtos;

import at.qe.skeleton.model.DateFormat;

public record UserSettingsPatchDTO (
    Boolean darkMode,
    Boolean fahrenheit,
    DateFormat format,
    Boolean twelveHourFormat
){
}
