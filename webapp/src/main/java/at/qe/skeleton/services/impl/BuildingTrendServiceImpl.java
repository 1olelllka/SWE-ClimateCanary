package at.qe.skeleton.services.impl;

import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.model.BuildingTrend;
import at.qe.skeleton.repositories.BuildingTrendRepository;
import at.qe.skeleton.services.BuildingTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuildingTrendServiceImpl implements BuildingTrendService {

    private final BuildingTrendRepository trendRepository;

    @Override
    public List<BuildingTrend> getTrendsForDepartment(UUID id, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) throw new ValidationException("You may not enter start date after end date.");
        return trendRepository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(id, startDate, endDate);
    }
}
