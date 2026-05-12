package at.qe.skeleton.services.impl;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.mappers.AggregatedStatsMapper;
import at.qe.skeleton.mappers.ClimateDataPointMapper;
import at.qe.skeleton.mappers.LimitMapper;
import at.qe.skeleton.model.AggregatedStats;
import at.qe.skeleton.model.ClimateStats;
import at.qe.skeleton.model.Granularity;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.repositories.AggregatedStatsRepository;
import at.qe.skeleton.repositories.ClimateStatsRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.services.ClimateStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Log
public class ClimateStatsServiceImpl implements ClimateStatsService {

    private final ClimateStatsRepository climateStatsRepository;
    private final AggregatedStatsRepository aggregatedStatsRepository;
    private final RoomMonitoringRepository roomMonitoringRepository;
    private final ClimateDataPointMapper climateMapper;
    private final AggregatedStatsMapper  aggregatedMapper;
    private final LimitMapper            limitMapper;

    // for current climate values (only 3 latest are shown)
    @Override
    public ClimateDataPointDTO getCurrentClimate(UUID roomId) {
        return climateStatsRepository
                .findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId)
                .map(climateMapper::mapTo)
                .orElseThrow(() -> new NotFoundException(
                        "No climate data found for room: " + roomId));
    }

    // for POST /measurements
    @Override
    @Transactional
    public void saveMeasurementBatch(MeasurementBatchDTO batch) {
        RoomMonitoring room = roomMonitoringRepository.findById(batch.roomId())
                .orElseThrow(() -> new NotFoundException(
                        "RoomMonitoring not found: " + batch.roomId()));
        log.info("The room was found.");
        double temp = 0;
        double hum = 0;
        double poll = 0;
        for (ReadingDTO r : batch.readings()) {
            switch (r.type()) {
                case TEMPERATURE -> temp = r.value();
                case HUMIDITY    -> hum  = r.value();
                case CO2         -> poll = r.value();
            }
        }
        log.info("Received: Temperature – " + temp + ", Humidity – " + hum + ", CO2 – " + poll + ".");
        climateStatsRepository.save(ClimateStats.builder()
                .roomMonitoring(room)
                .date(batch.timestamp())
                .tempVal(temp)
                .humVal(hum)
                .pollVal(poll)
                .build());
        log.info("Data was saved.");
    }

    // to get values for a specific timeframe for a specific room
    @Override
    public List<ClimateDataPointDTO> getOvertime(UUID roomId,
                                                 LocalDate startDate,
                                                 LocalDate endDate,
                                                 LocalTime startTime,
                                                 LocalTime endTime) {
        OffsetDateTime from = startDate.atTime(startTime != null ? startTime : LocalTime.MIDNIGHT)
                .atZone(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime to   = endDate.atTime(endTime != null ? endTime : LocalTime.MAX)
                .atZone(ZoneId.systemDefault()).toOffsetDateTime();

        return climateStatsRepository
                .findByRoomMonitoring_RoomIdAndDateBetween(roomId, from, to)
                .stream()
                .map(climateMapper::mapTo)
                .toList();
    }

    // Full granularity
    @Override
    public List<AggregatedDataPointDTO> getClimateHistoryFull(UUID roomId,
                                                              String timeframe,
                                                              String granularity) {
        LocalDate to   = LocalDate.now();
        LocalDate from = resolveFrom(timeframe, to);

        // monthly view still uses DAY grouping (hourly over a month is visually useless)
        boolean useHourGrouping = "HOUR".equals(granularity) && !"MONTH".equals(timeframe);

        if (useHourGrouping) {
            return groupRawByHour(roomId, from, to);
        }
        return groupRawByDay(roomId, from, to);
    }

    // Reduced granularity, always returns day averages (never raw data, privacy reasons)
    @Override
    public List<AggregatedDataPointDTO> getClimateHistoryReduced(UUID roomId,
                                                                 String timeframe) {
        LocalDate to   = LocalDate.now();
        LocalDate from = resolveFrom(timeframe, to);

        List<AggregatedStats> aggregated = aggregatedStatsRepository
                .findByRoomIdAndDateBetweenAndGranularity(roomId, from, to, Granularity.DAILY);
        if (!aggregated.isEmpty()) {
            return aggregated.stream().map(aggregatedMapper::mapTo).toList();
        }
        log.info("Aggregated Data was not found.");

        // this is just a fallback for now — should be removed once background job is running
        return groupRawByDay(roomId, from, to);
    }

    @Override
    public LimitDTO getLimits(UUID roomId) {
        RoomMonitoring room = roomMonitoringRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room monitoring not found: " + roomId));
        return limitMapper.mapTo(room);
    }


    private LocalDate resolveFrom(String timeframe, LocalDate to) {
        return switch (timeframe) {
            case "DAY"   -> to.minusDays(1);
            case "WEEK"  -> to.minusWeeks(1);
            case "MONTH" -> to.minusMonths(1);
            default -> throw new IllegalArgumentException("Invalid timeframe: " + timeframe);
        };
    }

    /**
     * Groups raw ClimateStats by hour and returns averages.
     * Used for HOUR granularity.
     */
    private List<AggregatedDataPointDTO> groupRawByHour(UUID roomId,
                                                        LocalDate from,
                                                        LocalDate to) {
        return climateStatsRepository
                .findByRoomMonitoring_RoomIdAndDateBetween(
                        roomId,
                        from.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime(),
                        to.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .stream()
                .collect(Collectors.groupingBy(s ->
                        s.getDate().truncatedTo(ChronoUnit.HOURS)))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toAggregatedDTO(entry.getKey().toLocalDate(), entry.getValue()))
                .toList();
    }

    /**
     * Groups raw ClimateStats by day and returns averages.
     * TEMPORARY ONLY!!!
     */
    private List<AggregatedDataPointDTO> groupRawByDay(UUID roomId,
                                                       LocalDate from,
                                                       LocalDate to) {
        return climateStatsRepository
                .findByRoomMonitoring_RoomIdAndDateBetween(
                        roomId,
                        from.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime(),
                        to.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .stream()
                .collect(Collectors.groupingBy(s -> s.getDate().toLocalDate()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toAggregatedDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    private AggregatedDataPointDTO toAggregatedDTO(LocalDate date, List<ClimateStats> group) {
        return new AggregatedDataPointDTO(
                date,
                average(group, ClimateStats::getTempVal),
                average(group, ClimateStats::getHumVal),
                average(group, ClimateStats::getPollVal)
        );
    }

    private double average(List<ClimateStats> group,
                           ToDoubleFunction<ClimateStats> extractor) {
        return group.stream().mapToDouble(extractor).average().orElse(0.0);
    }
}
