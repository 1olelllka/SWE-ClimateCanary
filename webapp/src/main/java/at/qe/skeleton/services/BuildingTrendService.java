package at.qe.skeleton.services;

import at.qe.skeleton.model.BuildingTrend;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BuildingTrendService {
    List<BuildingTrend> getTrendsForDepartment(UUID id, LocalDate startDate, LocalDate endDate);
}
