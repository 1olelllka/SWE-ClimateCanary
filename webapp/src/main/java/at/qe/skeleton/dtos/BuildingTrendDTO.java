package at.qe.skeleton.dtos;

import at.qe.skeleton.model.Trend;

import java.time.LocalDate;
import java.util.UUID;

public record BuildingTrendDTO(
        UUID id,
        UUID departmentId,
        String departmentName,
        Trend trend,
        Double value,
        LocalDate date
) {
}
