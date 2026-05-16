package at.qe.skeleton.services.impl;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.exceptions.ForbiddenException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.AggregatedStatsMapper;
import at.qe.skeleton.mappers.ClimateDataPointMapper;
import at.qe.skeleton.mappers.LimitMapper;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.AggregatedStatsRepository;
import at.qe.skeleton.repositories.ClimateStatsRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.ClimateStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ClimateStatsServiceImpl implements ClimateStatsService {

    private final ClimateStatsRepository climateStatsRepository;
    private final AggregatedStatsRepository aggregatedStatsRepository;
    private final RoomMonitoringRepository roomMonitoringRepository;
    private final RoomRepository roomRepository;
    private final ClimateDataPointMapper climateMapper;
    private final AggregatedStatsMapper  aggregatedMapper;
    private final LimitMapper            limitMapper;
    private final AuthenticatedUserService authenticatedUserService;

    // for current climate values (only 3 latest are shown)
    @Override
    public ClimateDataPointDTO getCurrentClimate(UUID roomId) {
        Userx authenticated = authenticatedUserService.getAuthenticatedUser();
        List<String> roles = authenticated.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room with id %s was not found.".formatted(roomId.toString())));
        boolean isDeptHead = roles.contains("CAN_VIEW_OWN_DEPARTMENT_MEASURES");
        boolean isEmployee = roles.contains("CAN_VIEW_OWN_OFFICE_CLIMATE");
        boolean isBuilding = roles.contains("CAN_VIEW_ALL_ROOMS");
        if (isEmployee && !isDeptHead && !isBuilding) {
            if (authenticated.getMyRoom() != null && authenticated.getMyRoom().getId().equals(room.getId()) && room.getRoomType() == RoomType.OFFICE)
                return climateStatsRepository
                        .findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId)
                        .map(climateMapper::mapTo)
                        .orElseThrow(() -> new NotFoundException(
                                "No climate data found for room: %s".formatted(roomId.toString())));
            else if (authenticated.getMyRoom() != null && authenticated.getMyRoom().getDepartment().getId().equals(room.getDepartment().getId()) && room.getRoomType() == RoomType.SHARED) {
                return climateStatsRepository
                        .findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId)
                        .map(climateMapper::mapTo)
                        .orElseThrow(() -> new NotFoundException(
                                "No climate data found for room: %s".formatted(roomId.toString())));
            }
            throw new ForbiddenException("You are not allowed to see other's rooms.");
        }
        if (isDeptHead && !isBuilding) {
            if (authenticated.getMyRoom() != null && authenticated.getMyRoom().getDepartment().getId().equals(room.getDepartment().getId())) {
                return climateStatsRepository
                        .findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId)
                        .map(climateMapper::mapTo)
                        .orElseThrow(() -> new NotFoundException(
                                "No climate data found for room: " + roomId));
            }
            throw new ForbiddenException("You are not allowed to see other's rooms.");
        }
        if (isBuilding) {
            return climateStatsRepository
                    .findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId)
                    .map(climateMapper::mapTo)
                    .orElseThrow(() -> new NotFoundException(
                            "No climate data found for room: " + roomId));
        }
        throw new ForbiddenException("You are not allowed to see other's rooms.");
    }

    // for POST /measurements
    @Override
    @Transactional
    public void saveMeasurementBatch(MeasurementBatchDTO batch) {
        RoomMonitoring room = roomMonitoringRepository.findById(batch.roomId())
                .orElseThrow(() -> new NotFoundException(
                        "RoomMonitoring not found: " + batch.roomId()));
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
        log.info("Received: Temperature – {}, Humidity – {}, CO2 – {}", temp, hum, poll);
        climateStatsRepository.save(ClimateStats.builder()
                .roomMonitoring(room)
                .date(batch.timestamp())
                .tempVal(temp)
                .humVal(hum)
                .pollVal(poll)
                .build());
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
        if (to.isBefore(from) || from.plusDays(2).isBefore(to)) throw new ValidationException("Invalid timestamps.");
        Userx authenticated = authenticatedUserService.getAuthenticatedUser();
        List<String> roles = authenticated.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room with id %s was not found.".formatted(roomId.toString())));
        boolean isBuilding = roles.contains("CAN_VIEW_ALL_ROOMS");
        if (!isBuilding) {
            boolean sameDepartment = authenticated.getMyRoom() != null && authenticated.getMyRoom().getDepartment().getId()
                    .equals(room.getDepartment().getId());
            boolean sameRoom = authenticated.getMyRoom() != null && authenticated.getMyRoom().getId().equals(roomId);

            if (room.getRoomType().equals(RoomType.SHARED) && !sameDepartment)
                throw new ForbiddenException("You are not allowed to see others' room climate.");

            if (room.getRoomType().equals(RoomType.OFFICE) && !sameRoom) {
                throw new ForbiddenException("You are not allowed to see others' room climate.");
            }
        }
        return climateStatsRepository
                .findByRoomMonitoring_RoomIdAndDateBetween(roomId, from, to)
                .stream()
                .map(climateMapper::mapTo)
                .toList();
    }

    // Full granularity
    @Override
    public List<AggregatedDataPointDTO> getClimateHistoryFull(UUID roomId,
                                                              LocalDate from,
                                                              LocalDate to,
                                                              String granularity) {
        if (from.isAfter(to)) throw new ValidationException("Invalid timestamps.");
        Userx user = authenticatedUserService.getAuthenticatedUser();
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room with id %s was not found.".formatted(roomId.toString())));
        List<String> roles = user.getAuthorities().stream().map(role -> role.getAuthority()).toList();
        boolean isBuilding = roles.contains("CAN_VIEW_ALL_ROOMS");
        if (!isBuilding) {
            if (user.getMyRoom() == null || room.getRoomType().equals(RoomType.SHARED) &&!user.getMyRoom().getDepartment().getId().equals(room.getDepartment().getId())) {
                throw new ForbiddenException("You are not allowed to see other's rooms.");
            }
            if (room.getRoomType().equals(RoomType.OFFICE) && !user.getMyRoom().getId().equals(room.getId())) {
                throw new ForbiddenException("You are not allowed to see other's rooms.");
            }
        }
        boolean useHourGrouping = "HOUR".equals(granularity) && ChronoUnit.DAYS.between(from, to) <= 4;
        boolean useDayGrouping = "DAY".equals(granularity) || ChronoUnit.DAYS.between(from, to) > 4 && ChronoUnit.DAYS.between(from, to) < 45;
        // TEMPORARY VISUALIZATIONS – Background jobs should work instead
        List<AggregatedStats> data;
        if (useHourGrouping)
            return groupRawByHour(roomId, from, to);
        else if (useDayGrouping && !"WEEK".equals(granularity)) {
            data = aggregatedStatsRepository
                    .findByRoomIdAndDateBetweenAndGranularity(roomId, from, to, Granularity.DAILY);
            if (!data.isEmpty()) {
                return data.stream().map(aggregatedMapper::mapTo).toList();
            }
            log.info("Aggregated Data was not found.");
            return groupRawByDay(roomId, from, to);
        } else {
            data = aggregatedStatsRepository
                    .findByRoomIdAndDateBetweenAndGranularity(roomId, from, to, Granularity.WEEKLY);
            if (!data.isEmpty()) {
                return data.stream().map(aggregatedMapper::mapTo).toList();
            }
            log.info("Aggregated Data was not found.");
            return groupRawByWeek(roomId, from, to);
        }
    }

    // Reduced granularity, always returns day averages (never raw data, privacy reasons)
    @Override
    public List<AggregatedDataPointDTO> getClimateHistoryReduced(UUID roomId,
                                                                 LocalDate from,
                                                                 LocalDate to,
                                                                 String granularity) {
        if (from.isAfter(to)) throw new ValidationException("Invalid timestamps.");
        boolean weekly = "WEEK".equals(granularity) && ChronoUnit.DAYS.between(from, to) > 45;
        boolean hourly = "HOUR".equals(granularity) && ChronoUnit.DAYS.between(from, to) <= 2;
        Userx authenticatedDeptMan = authenticatedUserService.getAuthenticatedUser();
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room with id %s was not found.".formatted(roomId.toString())));
        if (authenticatedDeptMan.getMyRoom() == null || !authenticatedDeptMan.getMyRoom().getDepartment().getId().equals(room.getDepartment().getId()))
            throw new ForbiddenException("You are not allowed to see measures for this department.");
        if (!authenticatedDeptMan.getMyRoom().getId().equals(room.getId()) && room.getRoomType() != RoomType.SHARED && hourly)
            throw new ForbiddenException("You are not allowed to see detailed measures for this room.");
        List<AggregatedStats> aggregated;
        if (weekly) {
            aggregated = aggregatedStatsRepository
                    .findByRoomIdAndDateBetweenAndGranularity(roomId, from, to, Granularity.WEEKLY);
        } else {
            aggregated = aggregatedStatsRepository
                    .findByRoomIdAndDateBetweenAndGranularity(roomId, from, to, Granularity.DAILY);
        }
        if (!aggregated.isEmpty()) {
            return aggregated.stream().map(aggregatedMapper::mapTo).toList();
        }
        log.info("Aggregated Data not found.");

        // this is just a fallback for now — should be removed once background job is running
        if (hourly) {
            return groupRawByHour(roomId, from, to);
        }
        else if (weekly) {
            return groupRawByWeek(roomId, from, to);
        } else
            return groupRawByDay(roomId, from, to);
    }

    @Override
    public LimitDTO getLimits(UUID roomId) {
        RoomMonitoring room = roomMonitoringRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room monitoring not found: " + roomId));
        return limitMapper.mapTo(room);
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

    private List<AggregatedDataPointDTO> groupRawByWeek(UUID roomId,
                                                        LocalDate from,
                                                        LocalDate to) {
        return climateStatsRepository
                .findByRoomMonitoring_RoomIdAndDateBetween(
                        roomId,
                        from.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime(),
                        to.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .stream()
                .collect(Collectors.groupingBy(s -> s.getDate().toLocalDate()
                        .with(WeekFields.ISO.dayOfWeek(), 1))) // group by Monday of that week
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
