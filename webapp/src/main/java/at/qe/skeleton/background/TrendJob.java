package at.qe.skeleton.background;

import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.AggregatedStatsRepository;
import at.qe.skeleton.repositories.BuildingTrendRepository;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.repositories.FormulaWeightsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Background job that computes a daily climate quality score (trend value) for each
 * department and persists it as a {@link BuildingTrend}. The score is derived from the
 * most recent daily room aggregations using a configurable weighted formula over
 * temperature, humidity, and CO₂. The direction compared to the previous score is
 * stored as {@link Trend#UP}, {@link Trend#DOWN}, or {@link Trend#STABLE}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TrendJob {

    private final BuildingTrendRepository trendRepository;
    private final AggregatedStatsRepository aggregatedStatsRepository;
    private final DepartmentRepository departmentRepository;
    private final FormulaWeightsRepository weightsRepository;

    @Value("${app.aggregation.run-on-startup:true}")
    private boolean runOnStartup;

    /**
     * Executes an initial trend calculation when the application starts, if
     * {@code app.aggregation.run-on-startup} is {@code true} (default).
     */
    @PostConstruct
    void init() {
        if (runOnStartup) {
            trendDaily();
        }
    }

    /**
     * Calculates and saves a {@link BuildingTrend} for every department based on the
     * latest available daily {@link AggregatedStats} per room. Departments with no room
     * data are assigned a score of {@code 0} and marked {@link Trend#STABLE}.
     * Scheduled via {@code app.aggregation.daily.cron} (default: 01:00 every day).
     */
    @Scheduled(cron = "${app.aggregation.daily.cron:0 0 1 * * *}")
    @Async
    @Transactional
    public void trendDaily() {
        log.info("Running background daily trend calculation...");
        List<Department> departments = departmentRepository.findAllWithRooms();
        for (Department department : departments) {
            double value = 0;
            int roomsWithData = 0;
            Trend trending;
            for (Room room : department.getRooms()) {
                AggregatedStats roomStats = aggregatedStatsRepository.findFirstByRoomIdAndGranularityOrderByDateDesc(room.getId(), Granularity.DAILY);
                if (roomStats == null) {
                    log.info("Aggregated stats for room {} {} was not found.", room.getId().toString(), room.getRoomNumber());
                    continue;
                }
                value += avgFormula(roomStats.getAvgTemp(), roomStats.getAvgHumidity(), roomStats.getAvgCO2());
                roomsWithData++;
            }
            if (roomsWithData > 0) value = value / roomsWithData;
            BuildingTrend lastOne = trendRepository.findFirstByDepartmentIdOrderByDateDesc(department.getId());
            if (lastOne == null) {
                trending = Trend.STABLE;
            } else {
                if (lastOne.getValue() > value) trending = Trend.DOWN;
                else if (lastOne.getValue() < value) trending = Trend.UP;
                else trending = Trend.STABLE;
            }
            BuildingTrend trend = BuildingTrend
                    .builder()
                    .departmentId(department.getId())
                    .departmentName(department.getName())
                    .value(value)
                    .trend(trending)
                    .date(LocalDate.now())
                    .build();
            trendRepository.save(trend);
        }
        log.info("Completed background daily trend calculation...");
    }

    /**
     * Computes a normalized climate quality score in the range {@code [0, 100]} from
     * the given temperature, humidity, and CO₂ readings. Higher scores indicate better
     * air quality. Weights are loaded from the database; if none are configured, the
     * default weights {@code 0.4 / 0.3 / 0.3} are used.
     *
     * @param temperature the average room temperature in °C
     * @param humidity    the average relative humidity in %
     * @param co2         the average CO₂ concentration in ppm
     * @return a quality score between {@code 0.0} and {@code 100.0}
     */
    private double avgFormula(double temperature, double humidity, double co2) {
        double normalizedTemp = (temperature - 18.0) / (26.0 - 18.0);
        double normalizedHumidity = (humidity - 30.0) / (70.0 - 30.0);
        double normalizedCo2 = (co2 - 400.0) / (2000.0 - 400.0);
        List<FormulaWeights> weights = weightsRepository.findAll();
        double raw;
        if (weights.isEmpty())
            raw = 0.4 * normalizedTemp + 0.3 * normalizedHumidity + 0.3 * normalizedCo2;
        else {
            raw = weights.getFirst().getTempWeight() * normalizedTemp + weights.getFirst().getHumWeight() * normalizedHumidity + weights.getFirst().getCo2Weight() * normalizedCo2;
        }
        return Math.max(0.0, Math.min(100.0, (1.0 - raw) * 100.0));
    }

}