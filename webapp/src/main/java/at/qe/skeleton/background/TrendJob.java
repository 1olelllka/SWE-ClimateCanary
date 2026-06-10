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

    @PostConstruct
    void init() {
        if (runOnStartup) {
            trendDaily();
        }
    }

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

    private double avgFormula(double temperature, double humidity, double co2) {
        double normalizedTemp = (temperature - 18.0) / (26.0 - 18.0);
        double normalizedHumidity = (humidity - 30.0) / (70.0 - 30.0);
        double normalizedCo2 = (co2 - 400.0) / (2000.0 - 400.0);
        List<FormulaWeights> weights = weightsRepository.findAll();
        double raw;
        log.info(weights.toString());
        if (weights.isEmpty())
            raw = 0.4 * normalizedTemp + 0.3 * normalizedHumidity + 0.3 * normalizedCo2;
        else{
            raw = weights.getFirst().getTempWeight() * normalizedTemp + weights.getFirst().getHumWeight() * normalizedHumidity + weights.getFirst().getCo2Weight() * normalizedCo2;
        }
        return Math.max(0.0, Math.min(100.0, (1.0 - raw) * 100.0));
    }

}
