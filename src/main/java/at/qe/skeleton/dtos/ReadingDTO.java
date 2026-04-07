package at.qe.skeleton.dtos;

import at.qe.skeleton.model.MeasurementType;

public record ReadingDTO(
        MeasurementType type,

        double value
) {
}
