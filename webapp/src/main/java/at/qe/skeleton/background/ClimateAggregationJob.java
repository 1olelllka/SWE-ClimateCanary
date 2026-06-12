package at.qe.skeleton.background;

import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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

/**
 * Background job responsible for aggregating raw climate sensor data into daily and
 * weekly summaries, both per room and per department. Runs automatically at midnight
 * (daily) and on Sundays (weekly), and optionally on application startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClimateAggregationJob {

    private final ClimateStatsRepository climateStatsRepository;
    private final AggregatedStatsRepository aggregatedStatsRepository;
    private final RoomMonitoringRepository roomMonitoringRepository;
    private final AggregatedDepartmentStatsRepository departmentStatsRepository;
    private final DepartmentRepository departmentRepository;

    @Value("${app.aggregation.run-on-startup:true}")
    private boolean runOnStartup;

    /**
     * Triggered once the application context is fully started. Kicks off both daily
     * and weekly aggregation passes when {@code app.aggregation.run-on-startup} is
     * {@code true} (default).
     */
    @EventListener(ApplicationReadyEvent.class)
    void init() {
        if (runOnStartup) {
            aggregateDaily();
            aggregateWeekly();
        }
    }

    /**
     * Aggregates yesterday's raw {@link ClimateStats} records into a single
     * {@link AggregatedStats} row per room (granularity {@code DAILY}), and computes
     * the daily averages for each department. Scheduled via
     * {@code app.aggregation.daily.cron} (default: midnight every day).
     */
    @Scheduled(cron = "${app.aggregation.daily.cron:0 0 0 * * *}")
    @Async
    @Transactional
    public void aggregateDaily() {
        log.info("Running daily climate aggregation...");
        getRooms().forEach(roomId -> aggregateDay(roomId, LocalDate.now().minusDays(1)));
        getDepartments().forEach(department -> aggregateDayForDepartment(department, LocalDate.now().minusDays(1)));
    }

    /**
     * Builds a {@link AggregatedDepartmentStats} entry for the given department and
     * date by averaging the already-computed daily room stats. Skips the computation
     * if a record for that department and date already exists.
     *
     * @param department the department to aggregate
     * @param localDate  the date to aggregate for
     */
    private void aggregateDayForDepartment(Department department, LocalDate localDate) {
        log.info("Running aggregation for department {} {}.", department.getId(), department.getName());
        if (departmentStatsRepository.existsByDepartmentIdAndDate(department.getId(), localDate)) {
            log.info("Daily aggregation already exists for department with id {}", department.getId());
            return;
        }
        AggregatedDepartmentStats stats = AggregatedDepartmentStats
                .builder()
                .departmentId(department.getId())
                .date(localDate)
                .avgCO2(0)
                .avgHumidity(0)
                .avgTemp(0)
                .build();
        for (Room room : department.getRooms()) {
            Optional<AggregatedStats> roomStats = aggregatedStatsRepository.findFirstByRoomIdAndDateAndGranularity(room.getId(), localDate, Granularity.DAILY);
            if (roomStats.isEmpty()) {
                log.info("Not found aggregated stats for room {}, department {}.", room.getRoomNumber(), department.getId());
                continue;
            }
            stats.setAvgCO2(stats.getAvgCO2() + roomStats.get().getAvgCO2());
            stats.setAvgTemp(stats.getAvgTemp() + roomStats.get().getAvgTemp());
            stats.setAvgHumidity(stats.getAvgHumidity() + roomStats.get().getAvgHumidity());
        }
        stats.setAvgCO2(stats.getAvgCO2() / department.getRooms().size());
        stats.setAvgHumidity(stats.getAvgHumidity() / department.getRooms().size());
        stats.setAvgTemp(stats.getAvgTemp() / department.getRooms().size());
        departmentStatsRepository.save(stats);
    }

    /**
     * Averages the 7 {@code DAILY} rows from the past week into a single {@code WEEKLY}
     * summary per room. Scheduled via {@code app.aggregation.weekly.cron} (default:
     * midnight every Sunday).
     */
    @Scheduled(cron = "${app.aggregation.weekly.cron:0 0 0 * * SUN}")
    @Async
    @Transactional
    public void aggregateWeekly() {
        getRooms().forEach(roomId -> {
            LocalDate weekStart = LocalDate.now().minusDays(7);
            LocalDate weekEnd = LocalDate.now().minusDays(1);

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

    /**
     * Computes and persists a {@code DAILY} {@link AggregatedStats} entry for the
     * given room and date. Skips if an entry already exists or if there are no raw
     * records for that day.
     *
     * @param roomId the UUID of the room to aggregate
     * @param date   the date to aggregate
     */
    private void aggregateDay(UUID roomId, LocalDate date) {
        log.info("Running daily aggregation for room {}", roomId);
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

    /**
     * Returns the UUIDs of all monitored rooms.
     *
     * @return list of room UUIDs derived from all {@link RoomMonitoring} entries
     */
    private List<UUID> getRooms() {
        return roomMonitoringRepository.findAll().stream().map(RoomMonitoring::getRoomId).toList();
    }

    /**
     * Returns all departments including their associated rooms.
     *
     * @return list of {@link Department} entities with rooms eagerly loaded
     */
    private List<Department> getDepartments() {
        return departmentRepository.findAllWithRooms();
    }

    /**
     * Computes the arithmetic mean of a numeric property across a list of items.
     *
     * @param <T>       the element type
     * @param list      the list to average over
     * @param extractor function that extracts the numeric value from each element
     * @return the average, or {@code 0.0} if the list is empty
     */
    private <T> double avg(List<T> list, ToDoubleFunction<T> extractor) {
        return list.stream().mapToDouble(extractor).average().orElse(0.0);
    }
}