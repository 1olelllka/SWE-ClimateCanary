package at.qe.skeleton.services;

import at.qe.skeleton.model.AggregatedStats;
import at.qe.skeleton.model.Granularity;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.repositories.AggregatedStatsRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AggregatedStatsSeederService {

    private final AggregatedStatsRepository aggregatedStatsRepository;
    private final RoomMonitoringRepository roomMonitoringRepository;

    @Value("${app.seeder.climate-history.days:35}")
    private int historyDays;

    public void seed() {
        log.info("Running aggregated stats seeder...");
        LocalDate cutoff = LocalDate.now().minusDays(2);
        if (aggregatedStatsRepository.existsByDateBeforeAndGranularity(cutoff, Granularity.DAILY)) {
            log.info("Aggregated stats already seeded. Aborting...");
            return;
        }

        List<RoomMonitoring> rooms = roomMonitoringRepository.findAll();
        if (rooms.isEmpty()) {
            log.warn("No rooms found, skipping aggregated stats seeding.");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(historyDays);
        log.info("Seeding {} days of aggregated DAILY stats for {} room(s)...", historyDays, rooms.size());

        for (int i = 0; i < rooms.size(); i++) {
            UUID roomId = rooms.get(i).getRoomId();
            double tempOffset = ((i % 5) - 2) * 0.8;
            double humOffset  = ((i % 3) - 1) * 4.0;

            List<AggregatedStats> batch = new ArrayList<>();
            for (LocalDate date = start; date.isBefore(today); date = date.plusDays(1)) {
                if (aggregatedStatsRepository.existsByRoomIdAndDateAndGranularity(roomId, date, Granularity.DAILY)) {
                    continue;
                }
                batch.add(AggregatedStats.builder()
                        .roomId(roomId)
                        .date(date)
                        .granularity(Granularity.DAILY)
                        .avgTemp((float) dailyAvgTemp(date, tempOffset))
                        .avgHumidity((float) dailyAvgHum(date, tempOffset, humOffset))
                        .avgCO2((float) dailyAvgCO2(date))
                        .build());
            }

            if (!batch.isEmpty()) {
                aggregatedStatsRepository.saveAll(batch);
            }
            log.info("Seeded room {} ({}/{})", rooms.get(i).getRoomNumber(), i + 1, rooms.size());
        }

        log.info("Aggregated stats seeding complete.");
    }

    // Daily average temperature: sinusoidal average is ~0 so base ≈ 21°C + seasonal + room offset
    private double dailyAvgTemp(LocalDate date, double roomOffset) {
        double seasonal = 3.0 * Math.sin(2 * Math.PI * (date.getDayOfYear() - 80) / 365.0);
        double workday  = date.getDayOfWeek().getValue() <= 5 ? 0.3 : -0.2;
        double noise    = deterministicNoise(date, 0) * 1.2;
        return clamp(21.0 + seasonal + workday + roomOffset + noise, 15.0, 30.0);
    }

    private double dailyAvgHum(LocalDate date, double tempOffset, double humOffset) {
        double temp      = dailyAvgTemp(date, tempOffset);
        double tempFactor = -(temp - 21.0) * 1.5;
        double noise     = deterministicNoise(date, 1) * 5.0;
        return clamp(50.0 + tempFactor + humOffset + noise, 25.0, 75.0);
    }

    private double dailyAvgCO2(LocalDate date) {
        double base  = date.getDayOfWeek().getValue() >= 6 ? 12.0 : 28.0;
        double noise = deterministicNoise(date, 2) * 8.0;
        return clamp(base + noise, 2.0, 90.0);
    }

    // Deterministic value in [-0.5, 0.5] derived from the date so values are stable across restarts
    private double deterministicNoise(LocalDate date, int salt) {
        int hash = (date.hashCode() ^ (salt * 0x9e3779b9));
        return ((hash & 0xFF) / 255.0) - 0.5;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
