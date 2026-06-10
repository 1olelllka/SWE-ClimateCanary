package at.qe.skeleton.dtos;

import java.time.LocalDateTime;

public record FormulaWeightDTO(
        Double tempWeight,
        Double co2Weight,
        Double humWeight,
        LocalDateTime modifiedAt
) {
}
