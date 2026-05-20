package at.qe.skeleton.services;

import at.qe.skeleton.model.AggregatedDepartmentStats;
import at.qe.skeleton.model.BuildingTrend;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Trend;
import at.qe.skeleton.repositories.AggregatedDepartmentStatsRepository;
import at.qe.skeleton.repositories.BuildingTrendRepository;
import at.qe.skeleton.repositories.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingTrendSeederService {

    private final DepartmentRepository departmentRepository;
    private final AggregatedDepartmentStatsRepository departmentStatsRepository;
    private final BuildingTrendRepository buildingTrendRepository;

    @Transactional
    public void seed() {
        log.info("Running building trend seeder...");

        if (buildingTrendRepository.count() > 0) {
            log.info("Building trends already seeded. Aborting...");
            return;
        }

        List<Department> departments = departmentRepository.findAll();
        if (departments.isEmpty()) {
            log.warn("No departments found — skipping building trend seeding.");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(364);

        List<BuildingTrend> batch = new ArrayList<>();

        for (Department dept : departments) {
            List<AggregatedDepartmentStats> stats =
                    departmentStatsRepository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(
                            dept.getId(), start, today);

            if (stats.isEmpty()) {
                log.warn("No aggregated stats for department '{}', skipping.", dept.getName());
                continue;
            }

            double prevValue = -1;

            for (AggregatedDepartmentStats s : stats) {
                double value = compositeScore(s.getAvgTemp(), s.getAvgHumidity(), s.getAvgCO2());

                Trend trend;
                if (prevValue < 0) {
                    trend = Trend.STABLE;
                } else if (value > prevValue) {
                    trend = Trend.UP;
                } else if (value < prevValue) {
                    trend = Trend.DOWN;
                } else {
                    trend = Trend.STABLE;
                }

                batch.add(BuildingTrend.builder()
                        .departmentId(dept.getId())
                        .departmentName(dept.getName())
                        .value(value)
                        .trend(trend)
                        .date(s.getDate())
                        .build());

                prevValue = value;
            }

            log.info("Prepared {} trend records for department '{}'.", stats.size(), dept.getName());
        }

        buildingTrendRepository.saveAll(batch);
        log.info("Building trend seeding complete — {} records saved.", batch.size());
    }

    private double compositeScore(double temperature, double humidity, double co2) {
        double normalizedTemp     = (temperature - 18.0) / (26.0 - 18.0);
        double normalizedHumidity = (humidity    - 30.0) / (70.0 - 30.0);
        double normalizedCo2      = (co2         - 400.0) / (2000.0 - 400.0);
        return 0.4 * normalizedTemp + 0.3 * normalizedHumidity + 0.3 * normalizedCo2;
    }
}
