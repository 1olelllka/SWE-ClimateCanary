package at.qe.skeleton.dtos;

import at.qe.skeleton.model.WarningStatus;

import java.util.UUID;

public record DepartmentClimateSummaryDTO(
        UUID departmentId,

        double avgTemperature,

        double avgHumidity,

        double avgCO2,

        int activeWarnings,

        WarningStatus status
) {
}
