package at.qe.skeleton.services.impl;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.exceptions.ForbiddenException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.AggregatedStatsMapper;
import at.qe.skeleton.mappers.ClimateDataPointMapper;
import at.qe.skeleton.mappers.LimitMapper;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.ClimateStatsService;
import at.qe.skeleton.services.LiveDataService;
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

/**
 * Implementation of {@link ClimateStatsService} providing access to raw and aggregated
 * climate data with role-based visibility enforcement.
 *
 * <p>Access-control tiers:
 * <ul>
 *   <li>{@code CAN_VIEW_ALL_ROOMS} — building manager, unrestricted access to any room.</li>
 *   <li>{@code CAN_VIEW_OWN_DEPARTMENT_MEASURES} — department head, restricted to rooms
 *       in their own department.</li>
 *   <li>{@code CAN_VIEW_OWN_OFFICE_CLIMATE} — employee, restricted to their own office
 *       room and shared rooms in their department.</li>
 * </ul>
 *
 * <p>History methods ({@link #getClimateHistoryFull}, {@link #getClimateHistoryReduced})
 * prefer pre-computed {@link AggregatedStats} rows produced by
 * {@link at.qe.skeleton.background.ClimateAggregationJob} and fall back to grouping raw
 * {@link ClimateStats} records on the fly when aggregated data is absent.
 * The raw-grouping fallbacks ({@link #groupRawByDay}, {@link #groupRawByWeek}) are
 * temporary and should be removed once the background job runs reliably.
 */
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
    private final AggregatedDepartmentStatsRepository departmentStatsRepository;
    private final LiveDataService liveDataService;

    /**
     * Returns the most recent climate reading for the given room. Access is enforced
     * based on the authenticated user's role: employees may only query their own office
     * or shared rooms in their department; department heads may query any room in their
     * department; building managers have unrestricted access.
     *
     * @param roomId the UUID of the room to query
     * @return the latest {@link ClimateDataPointDTO} for the room
     * @throws NotFoundException  if the room does not exist or has no climate data
     * @throws ForbiddenException if the user is not permitted to view this room's data
     */
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
                        .orElseThrow(() -> {
                            log.info("No climate data found for room: {}", roomId);
                            return new NotFoundException(
                                    "No climate data found for room: %s".formatted(roomId.toString()));
                        });
            else if (authenticated.getMyRoom() != null && authenticated.getMyRoom().getDepartment().getId().equals(room.getDepartment().getId()) && room.getRoomType() == RoomType.SHARED) {
                return climateStatsRepository
                        .findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId)
                        .map(climateMapper::mapTo)
                        .orElseThrow(() -> {
                            log.info("No climate data found for room: {}", roomId);
                            return new NotFoundException(
                                    "No climate data found for room: %s".formatted(roomId.toString()));
                        });
            }
            throw new ForbiddenException("You are not allowed to see other's rooms.");
        }
        if (isDeptHead && !isBuilding) {
            if (authenticated.getMyRoom() != null && authenticated.getMyRoom().getDepartment().getId().equals(room.getDepartment().getId())) {
                return climateStatsRepository
                        .findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId)
                        .map(climateMapper::mapTo)
                        .orElseThrow(() -> {
                            log.info("No climate data found for room: {}", roomId);
                            return new NotFoundException(
                                    "No climate data found for room: " + roomId);
                        });
            }
            throw new ForbiddenException("You are not allowed to see other's rooms.");
        }
        if (isBuilding) {
            return climateStatsRepository
                    .findTopByRoomMonitoring_RoomIdOrderByDateDesc(roomId)
                    .map(climateMapper::mapTo)
                    .orElseThrow(() -> {
                        log.info("No climate data found for room: {}", roomId);
                        return new NotFoundException(
                                "No climate data found for room: " + roomId);
                    });
        }
        throw new ForbiddenException("You are not allowed to see other's rooms.");
    }

    /**
     * Persists a batch of raw sensor readings from a Raspberry Pi and pushes the
     * saved data point to WebSocket subscribers via {@link LiveDataService}.
     * Each batch contains at most one reading per sensor type (temperature, humidity,
     * CO₂); missing types default to {@code 0}.
     *
     * @param batch the measurement batch reported by the Raspberry Pi
     * @throws NotFoundException if the room monitoring record cannot be found
     */
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
        log.info("Received from RaspberryPi: Temperature – {}, Humidity – {}, CO2 – {}", temp, hum, poll);
        ClimateStats res = climateStatsRepository.save(ClimateStats.builder()
                .roomMonitoring(room)
                .date(batch.timestamp())
                .tempVal(temp)
                .humVal(hum)
                .pollVal(poll)
                .build());
        liveDataService.pushLiveClimateData(room.getRoomId(), climateMapper.mapTo(res));
    }

    /**
     * Returns raw climate readings for the given room within a time window of at most
     * 2 days. Access rules mirror {@link #getCurrentClimate}: employees are restricted
     * to their own office or shared rooms in their department; building managers have
     * unrestricted access.
     *
     * @param roomId    the UUID of the room to query
     * @param startDate start date of the range (inclusive)
     * @param endDate   end date of the range (inclusive)
     * @param startTime optional start time; defaults to midnight if {@code null}
     * @param endTime   optional end time; defaults to end-of-day if {@code null}
     * @return list of {@link ClimateDataPointDTO}s within the window
     * @throws ValidationException if the range is invalid or exceeds 2 days
     * @throws NotFoundException   if the room does not exist
     * @throws ForbiddenException  if the user is not permitted to view this room's data
     */
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

    /**
     * Returns aggregated climate history for users with full access (department heads
     * and building managers). Granularity is selected automatically based on the
     * requested range and the {@code granularity} hint:
     * <ul>
     *   <li>{@code HOUR} with range ≤ 4 days → hourly grouping of raw data.</li>
     *   <li>{@code DAY} or range 4–44 days → daily aggregated rows (falls back to
     *       raw-by-day grouping if no aggregated data exists).</li>
     *   <li>{@code WEEK} or range ≥ 45 days → weekly aggregated rows (falls back to
     *       raw-by-week grouping if no aggregated data exists).</li>
     * </ul>
     *
     * @param roomId      the UUID of the room to query
     * @param from        start date (inclusive)
     * @param to          end date (inclusive)
     * @param granularity requested granularity hint: {@code "HOUR"}, {@code "DAY"}, or {@code "WEEK"}
     * @return list of {@link AggregatedDataPointDTO}s
     * @throws ValidationException if {@code from} is after {@code to}
     * @throws NotFoundException   if the room does not exist
     * @throws ForbiddenException  if the user is not permitted to view this room's data
     */
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
            if (user.getMyRoom() == null || room.getRoomType().equals(RoomType.SHARED) && !user.getMyRoom().getDepartment().getId().equals(room.getDepartment().getId())) {
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
            log.info("Daily aggregated data was not found.");
            return groupRawByDay(roomId, from, to);
        } else {
            data = aggregatedStatsRepository
                    .findByRoomIdAndDateBetweenAndGranularity(roomId, from, to, Granularity.WEEKLY);
            if (!data.isEmpty()) {
                return data.stream().map(aggregatedMapper::mapTo).toList();
            }
            log.info("Weekly aggregated Data was not found.");
            return groupRawByWeek(roomId, from, to);
        }
    }

    /**
     * Returns aggregated climate history with reduced granularity for department-level
     * users. Raw hourly data is never returned for other users' office rooms (privacy).
     * Prefers pre-computed aggregated rows; falls back to raw grouping when absent.
     *
     * @param roomId      the UUID of the room to query
     * @param from        start date (inclusive)
     * @param to          end date (inclusive)
     * @param granularity requested granularity hint: {@code "HOUR"}, {@code "DAY"}, or {@code "WEEK"}
     * @return list of {@link AggregatedDataPointDTO}s
     * @throws ValidationException if {@code from} is after {@code to}
     * @throws NotFoundException   if the room does not exist
     * @throws ForbiddenException  if the user does not belong to the room's department,
     *                             or requests hourly data for another user's office room
     */
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

        // fallback — should be removed once background job is running
        if (hourly) {
            log.info("Hourly aggregated data not found.");
            return groupRawByHour(roomId, from, to);
        } else if (weekly) {
            log.info("Weekly aggregated data not found.");
            return groupRawByWeek(roomId, from, to);
        } else {
            log.info("Daily aggregated data not found.");
            return groupRawByDay(roomId, from, to);
        }
    }

    /**
     * Returns the sensor limit configuration for the given room.
     *
     * @param roomId the room UUID
     * @return a {@link LimitDTO} with the room's temperature, humidity, and CO₂ limits
     * @throws NotFoundException if no monitoring record exists for that room
     */
    @Override
    public LimitDTO getLimits(UUID roomId) {
        RoomMonitoring room = roomMonitoringRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.info("Room with id {} not found", roomId);
                    return new NotFoundException("Room monitoring not found: " + roomId);
                });
        return limitMapper.mapTo(room);
    }

    /**
     * Returns the most recent daily aggregated climate data point for the given department.
     *
     * @param departmentId the department UUID
     * @return an {@link AggregatedDataPointDTO} for the latest aggregated record
     * @throws NotFoundException if no aggregated data exists for the department
     */
    @Override
    public AggregatedDataPointDTO getDepartmentAggregatedData(UUID departmentId) {
        AggregatedDepartmentStats stats = departmentStatsRepository.findFirstByDepartmentIdOrderByDateDesc(departmentId)
                .orElseThrow(() -> {
                    log.info("There's no data for department with id {}", departmentId);
                    return new NotFoundException("There's no data for department %s".formatted(departmentId.toString()));
                });
        return new AggregatedDataPointDTO(stats.getDate(), stats.getAvgTemp(), stats.getAvgHumidity(), stats.getAvgCO2());
    }

    /**
     * Returns daily aggregated climate data for the given department within a date range
     * of at most 180 days, ordered by date ascending.
     *
     * @param departmentId the department UUID
     * @param startDate    start of the range (inclusive)
     * @param endDate      end of the range (inclusive)
     * @return list of {@link AggregatedDataPointDTO}s ordered by date
     * @throws ValidationException if {@code startDate} is after {@code endDate}, or the
     *                             range exceeds 180 days
     */
    @Override
    public List<AggregatedDataPointDTO> getDepartmentAggregatedDataInTimePeriod(UUID departmentId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) throw new ValidationException("Invalid timestamps.");
        if (ChronoUnit.DAYS.between(startDate, endDate) > 180) throw new ValidationException("The time interval is too big.");
        List<AggregatedDepartmentStats> stats = departmentStatsRepository.findAllByDepartmentIdAndDateBetweenOrderByDateAsc(departmentId, startDate, endDate);
        return stats.stream().map(stat -> new AggregatedDataPointDTO(stat.getDate(), stat.getAvgTemp(), stat.getAvgHumidity(), stat.getAvgCO2())).toList();
    }

    /**
     * Groups raw {@link ClimateStats} records by hour and returns per-hour averages.
     * Used for {@code HOUR} granularity requests.
     *
     * @param roomId the room UUID
     * @param from   start date (inclusive)
     * @param to     end date (inclusive)
     * @return list of hourly {@link AggregatedDataPointDTO}s ordered by timestamp
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
     * Groups raw {@link ClimateStats} records by calendar day and returns per-day
     * averages. Temporary fallback — should be removed once the background aggregation
     * job runs reliably.
     *
     * @param roomId the room UUID
     * @param from   start date (inclusive)
     * @param to     end date (inclusive)
     * @return list of daily {@link AggregatedDataPointDTO}s ordered by date
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

    /**
     * Groups raw {@link ClimateStats} records by ISO week (keyed to the Monday of each
     * week) and returns per-week averages. Temporary fallback — should be removed once
     * the background aggregation job runs reliably.
     *
     * @param roomId the room UUID
     * @param from   start date (inclusive)
     * @param to     end date (inclusive)
     * @return list of weekly {@link AggregatedDataPointDTO}s ordered by week-start date
     */
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
                        .with(WeekFields.ISO.dayOfWeek(), 1)))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toAggregatedDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * Converts a date and a group of {@link ClimateStats} records into an
     * {@link AggregatedDataPointDTO} by averaging temperature, humidity, and CO₂.
     *
     * @param date  the representative date for the group
     * @param group the raw records to average
     * @return the aggregated data point
     */
    private AggregatedDataPointDTO toAggregatedDTO(LocalDate date, List<ClimateStats> group) {
        return new AggregatedDataPointDTO(
                date,
                average(group, ClimateStats::getTempVal),
                average(group, ClimateStats::getHumVal),
                average(group, ClimateStats::getPollVal)
        );
    }

    /**
     * Computes the arithmetic mean of a numeric property across a list of
     * {@link ClimateStats} records.
     *
     * @param group     the records to average
     * @param extractor function that extracts the numeric value from each record
     * @return the average, or {@code 0.0} if the list is empty
     */
    private double average(List<ClimateStats> group,
                           ToDoubleFunction<ClimateStats> extractor) {
        return group.stream().mapToDouble(extractor).average().orElse(0.0);
    }
}