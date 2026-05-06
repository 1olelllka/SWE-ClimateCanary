package at.qe.skeleton.background;

import at.qe.skeleton.model.AggregatedStats;
import at.qe.skeleton.model.ClimateStats;
import at.qe.skeleton.model.Granularity;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.repositories.AggregatedStatsRepository;
import at.qe.skeleton.repositories.ClimateStatsRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClimateAggregationJob {

    private final ClimateStatsRepository climateStatsRepository;
    private final AggregatedStatsRepository aggregatedStatsRepository;
    private final RoomMonitoringRepository roomMonitoringRepository;

    @Value("${app.aggregation.run-on-startup:true}")
    private boolean runOnStartup;

    @PostConstruct
    void init() {
        if (runOnStartup) {
            aggregateDaily();
            aggregateWeekly();
        }
    }

    @Scheduled(cron = "${app.aggregation.daily.cron:0 0 0 * * *}")
    @Async
    @Transactional
    public void aggregateDaily() {
        log.info("Running daily climate aggregation...");
        getRoomId().ifPresent(roomId -> aggregateDay(roomId, LocalDate.now().minusDays(1)));
    }

    // Averages the 7 DAILY rows from the past week into a single WEEKLY summary
    @Scheduled(cron = "${app.aggregation.weekly.cron:0 0 0 * * SUN}")
    @Async
    @Transactional
    public void aggregateWeekly() {
        log.info("Running weekly climate aggregation...");
        getRoomId().ifPresent(roomId -> {
            LocalDate weekStart = LocalDate.now().minusDays(7);
            LocalDate weekEnd   = LocalDate.now().minusDays(1);

            if (aggregatedStatsRepository.existsByRoomIdAndDateAndGranularity(roomId, weekStart, Granularity.WEEKLY)) {
                log.info("Weekly aggregation already exists for {} starting {}, skipping.", roomId, weekStart);
                return;
            }

            List<AggregatedStats> dailies = aggregatedStatsRepository
                    .findByRoomIdAndDateBetweenAndGranularity(roomId, weekStart, weekEnd, Granularity.DAILY);

            if (dailies.isEmpty()) {
                log.warn("No daily aggregations found for room {} in range {} – {}, skipping weekly.", roomId, weekStart, weekEnd);
                return;
            }

            aggregatedStatsRepository.save(AggregatedStats.builder()
                    .roomId(roomId)
                    .date(weekStart)
                    .granularity(Granularity.WEEKLY)
                    .avgTemp((float) avg(dailies, AggregatedStats::getAvgTemp))
                    .avgHumidity((float) avg(dailies, AggregatedStats::getAvgHumidity))
                    .avgCO2((float) avg(dailies, AggregatedStats::getAvgCO2))
                    .build());

            log.info("Weekly aggregation saved for room {} ({} days averaged, week starting {}).",
                    roomId, dailies.size(), weekStart);
        });
    }

    private void aggregateDay(UUID roomId, LocalDate date) {
        if (aggregatedStatsRepository.existsByRoomIdAndDateAndGranularity(roomId, date, Granularity.DAILY)) {
            log.info("Daily aggregation already exists for {} on {}, skipping.", roomId, date);
            return;
        }

        List<ClimateStats> records = climateStatsRepository
                .findByRoomMonitoring_RoomIdAndDateBetween(
                        roomId,
                        OffsetDateTime.from(date.atStartOfDay(ZoneOffset.UTC)),
                        date.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC));

        if (records.isEmpty()) {
            log.warn("No climate data for room {} on {}, skipping.", roomId, date);
            return;
        }

        aggregatedStatsRepository.save(AggregatedStats.builder()
                .roomId(roomId)
                .date(date)
                .granularity(Granularity.DAILY)
                .avgTemp((float) avg(records, ClimateStats::getTempVal))
                .avgHumidity((float) avg(records, ClimateStats::getHumVal))
                .avgCO2((float) avg(records, ClimateStats::getPollVal))
                .build());

        log.info("Daily aggregation saved for room {} on {} ({} records).", roomId, date, records.size());
    }

    private Optional<UUID> getRoomId() {
        return roomMonitoringRepository.findAll().stream()
                .filter(r -> r.getRoomNumber().endsWith("101"))
                .findFirst()
                .map(RoomMonitoring::getRoomId);
    }

    private <T> double avg(List<T> list, ToDoubleFunction<T> extractor) {
        return list.stream().mapToDouble(extractor).average().orElse(0.0);
    }
}
