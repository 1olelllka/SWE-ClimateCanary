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

/**
 * Implementation of {@link BuildingTrendService} providing read access to
 * {@link BuildingTrend} records computed by
 * {@link at.qe.skeleton.background.TrendJob}.
 */
@Service
@RequiredArgsConstructor
public class BuildingTrendServiceImpl implements BuildingTrendService {

    private final BuildingTrendRepository trendRepository;

    /**
     * Returns all trend entries for the given department within the specified date
     * range, ordered by date ascending.
     *
     * @param id        the department UUID
     * @param startDate the start of the date range (inclusive)
     * @param endDate   the end of the date range (inclusive)
     * @return list of {@link BuildingTrend} entries ordered by date
     * @throws ValidationException if {@code startDate} is after {@code endDate}
     */
    @Override
    public List<BuildingTrend> getTrendsForDepartment(UUID id, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) throw new ValidationException("You may not enter start date after end date.");
        return trendRepository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(id, startDate, endDate);
    }
}