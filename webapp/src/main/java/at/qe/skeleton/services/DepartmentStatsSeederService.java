package at.qe.skeleton.services;

import at.qe.skeleton.model.AggregatedDepartmentStats;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.repositories.AggregatedDepartmentStatsRepository;
import at.qe.skeleton.repositories.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentStatsSeederService {

    private final DepartmentRepository departmentRepository;
    private final AggregatedDepartmentStatsRepository departmentStatsRepository;

    @Value("${app.seeder.department-stats.days:30}")
    private int historyDays;

    private static final Random RANDOM = new Random(99);

    // Per-department climate profile: {tempBase, tempRange, humBase, co2Base, co2Peak}
    // CO2 values are on the system scale (limit = 70); values above 70 trigger warnings
    private static final float[][] PROFILES = {
        // Engineering: warm + high CO2 (lots of servers/computers)
        { 23.5f, 2.0f, 45f, 90f, 20f },
        // Marketing: comfortable, well-ventilated
        { 21.5f, 1.5f, 52f, 65f, 15f },
        // Human Resources: slightly cool, higher humidity
        { 20.5f, 1.5f, 58f, 60f, 12f },
        // Finance: warm, borderline CO2 — will breach the 70 limit on some days
        { 24.5f, 2.5f, 44f, 95f, 25f },
        // Operations: widest variation, temperature violations on some days
        { 25.5f, 3.5f, 47f, 78f, 20f },
    };

    @Transactional
    public void seed() {
        log.info("Running department stats seeder...");

        if (departmentStatsRepository.count() > 0) {
            log.info("Department stats already seeded. Aborting...");
            return;
        }

        List<Department> departments = departmentRepository.findAll();
        if (departments.isEmpty()) {
            log.warn("No departments found — skipping department stats seeding.");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(historyDays - 1L);

        List<AggregatedDepartmentStats> batch = new ArrayList<>();

        for (int di = 0; di < departments.size(); di++) {
            Department dept = departments.get(di);
            float[] profile = PROFILES[di % PROFILES.length];

            float tempBase  = profile[0];
            float tempRange = profile[1];
            float humBase   = profile[2];
            float co2Base   = profile[3];
            float co2Peak   = profile[4];

            for (int day = 0; day < historyDays; day++) {
                LocalDate date = start.plusDays(day);

                // Sinusoidal weekly pattern: values peak mid-week (Wed = day 3)
                double weekCycle = Math.sin(Math.PI * (date.getDayOfWeek().getValue() - 1) / 5.0);

                float temp = round1(tempBase
                        + (float) (tempRange * weekCycle)
                        + (float) (RANDOM.nextGaussian() * 0.4));

                // Humidity inversely correlated with temperature
                float hum = round1(humBase
                        - (temp - tempBase) * 1.2f
                        + (float) (RANDOM.nextGaussian() * 2.5));
                hum = clamp(hum, 25f, 80f);

                float co2 = round1(co2Base
                        + (float) (co2Peak * weekCycle)
                        + (float) (RANDOM.nextGaussian() * 8));
                co2 = clamp(co2, 40f, 150f);

                batch.add(AggregatedDepartmentStats.builder()
                        .departmentId(dept.getId())
                        .date(date)
                        .avgTemp(temp)
                        .avgHumidity(hum)
                        .avgCO2(co2)
                        .build());
            }

            log.info("Prepared {} days of stats for department '{}'", historyDays, dept.getName());
        }

        departmentStatsRepository.saveAll(batch);
        log.info("Department stats seeding complete — {} records saved.", batch.size());
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private float round1(float v) {
        return Math.round(v * 10f) / 10f;
    }
}
