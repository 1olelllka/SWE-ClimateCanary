package at.qe.skeleton.services;

import at.qe.skeleton.model.ClimateStats;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.repositories.ClimateStatsRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClimateHistorySeederService {

    private final RoomMonitoringRepository roomMonitoringRepository;
    private final ClimateStatsRepository climateStatsRepository;

    @Value("${app.seeder.climate-history.days:140}")
    private int historyDays;

    private static final int BATCH_SIZE = 1000;
    private static final Random RANDOM = new Random(42);

    public void seed() {
        log.info("Running climate history seeder...");
        if (climateStatsRepository.count() > 0) {
            log.info("Climate history already seeded. Aborting...");
            return;
        }

        List<RoomMonitoring> allRooms = roomMonitoringRepository.findAll();

        List<RoomMonitoring> rooms = new ArrayList<>();
        allRooms.stream()
                .filter(r -> r.getRoomNumber().endsWith("101"))
                .limit(1)
                .findFirst()
                .ifPresent(rooms::add);
        allRooms.stream()
                .filter(r -> "ENG-103".equals(r.getRoomNumber()))
                .findFirst()
                .ifPresent(rooms::add);
        allRooms.stream()
                .filter(r -> "ENG-102".equals(r.getRoomNumber()))
                .findFirst()
                .ifPresent(rooms::add);

        if (rooms.isEmpty()) {
            log.warn("No target rooms found, skipping climate history seeding.");
            return;
        }

        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        OffsetDateTime start = now.minusDays(historyDays);
        long minutesTotal = ChronoUnit.MINUTES.between(start, now);

        log.info("Seeding {} days of climate history ({} data points) for {} room(s)...",
                historyDays, minutesTotal, rooms.size());

        for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
            RoomMonitoring room = rooms.get(roomIndex);
            double roomTempOffset = switch (roomIndex) {
                case 0 -> 0.0;   // 101 — baseline
                case 1 -> 1.5;   // ENG-103 — slightly warmer
                case 2 -> -1.0;  // ENG-102 — slightly cooler
                default -> 0.0;
            };
            double roomHumOffset = switch (roomIndex) {
                case 0 -> 0.0;
                case 1 -> -5.0;  // ENG-103 — drier
                case 2 -> 8.0;   // ENG-102 — more humid
                default -> 0.0;
            };

            List<ClimateStats> batch = new ArrayList<>(BATCH_SIZE);
            OffsetDateTime cursor = start;

            OffsetDateTime warningOnset = now.minusHours(2);

            while (!cursor.isAfter(now)) {
                // For the most recent 2 hours elevate temperature above the 26 °C limit
                // so the sparkline trend line visually breaches the dashed limit line.
                double temp = cursor.isAfter(warningOnset)
                        ? round2(Math.max(WarningSeederService.TEMP_MAX,
                                WarningSeederService.TRIGGERED_TEMP + RANDOM.nextGaussian() * 0.4))
                        : computeTemperature(cursor, roomTempOffset);
                double hum = computeHumidity(temp, roomHumOffset);
                double poll = computePollution(cursor);

                batch.add(ClimateStats.builder()
                        .tempVal(round2(temp))
                        .humVal(round2(hum))
                        .pollVal(round2(poll))
                        .date(cursor)
                        .roomMonitoring(room)
                        .build());

                if (batch.size() >= BATCH_SIZE) {
                    climateStatsRepository.saveAll(batch);
                    batch.clear();
                }

                cursor = cursor.plusMinutes(1);
            }

            if (!batch.isEmpty()) {
                climateStatsRepository.saveAll(batch);
            }

            log.info("Seeded room {} ({}/{})", room.getRoomNumber(), roomIndex + 1, rooms.size());
        }

        log.info("Climate history seeding complete.");
    }

    // Peak ~14:00, trough ~04:00, daily sinusoidal variation
    private double computeTemperature(OffsetDateTime dt, double roomOffset) {
        double hourFraction = dt.getHour() + dt.getMinute() / 60.0;
        double dailyCycle = 3.0 * Math.sin(2 * Math.PI * (hourFraction - 4.0) / 24.0);
        double noise = RANDOM.nextGaussian() * 0.3;
        return clamp(21.0 + dailyCycle + roomOffset + noise, 15.0, 32.0);
    }

    // Inversely correlated with temperature
    private double computeHumidity(double temp, double roomOffset) {
        double tempFactor = -(temp - 21.0) * 1.5;
        double noise = RANDOM.nextGaussian() * 3.0;
        return clamp(50.0 + tempFactor + roomOffset + noise, 25.0, 75.0);
    }

    // Higher during weekday work hours (8–18), low on weekends and at night
    private double computePollution(OffsetDateTime dt) {
        int hour = dt.getHour();
        boolean isWeekend = dt.getDayOfWeek().getValue() >= 6;
        boolean isWorkHours = hour >= 8 && hour < 18;
        double base = isWeekend ? 10.0 : 20.0;
        double workPeak = (!isWeekend && isWorkHours)
                ? 25.0 * Math.sin(Math.PI * (hour - 8.0) / 10.0)
                : 0.0;
        double noise = RANDOM.nextGaussian() * 5.0;
        return clamp(base + workPeak + noise, 2.0, 90.0);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
