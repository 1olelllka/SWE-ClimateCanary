package at.qe.skeleton.services.impl;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.exceptions.ForbiddenException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.WarningCreateMapper;
import at.qe.skeleton.mappers.WarningMapper;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.EmailService;
import at.qe.skeleton.services.LiveDataService;
import at.qe.skeleton.services.WarningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link WarningService} managing the full lifecycle of climate
 * {@link Warnings}: creation (reported by a Raspberry Pi), status updates, resolution,
 * and multi-scope queries.
 *
 * <p>Access-control tiers for warning queries:
 * <ul>
 *   <li>{@code CAN_VIEW_ALL_ROOMS} — building manager, unrestricted access.</li>
 *   <li>{@code CAN_VIEW_OWN_DEPARTMENT_WARNINGS} — department head, restricted to
 *       their own department.</li>
 *   <li>{@code CAN_VIEW_OWN_OFFICE_WARNINGS} — employee, active warnings only for
 *       their own room or shared rooms in their department.</li>
 * </ul>
 *
 * <p>On warning creation, a matching {@link Tip} is looked up by sensor type and
 * violation direction (OVER/UNDER) and attached if found. The warning is then pushed
 * to WebSocket subscribers and, for users who opted in, sent as an email notification.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class WarningServiceImpl implements WarningService {

    private final WarningRepository warningsRepository;
    private final RoomMonitoringRepository roomMonitoringRepository;
    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final DepartmentRepository departmentRepository;
    private final WarningMapper warningMapper;
    private final WarningCreateMapper warningCreateMapper;
    private final TipRepository tipRepository;
    private final LiveDataService liveDataService;
    private final UserxRepository userxRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final EmailService emailService;

    /**
     * Returns the warnings for a specific room, filtered by the authenticated user's
     * access level. Building managers can query any date range; department heads are
     * restricted to their own department; employees see only active warnings for their
     * own room or shared rooms in their department.
     *
     * @param user      the authenticated user making the request
     * @param roomId    the UUID of the room to query
     * @param active    if {@code true}, returns only unresolved warnings; otherwise
     *                  returns all warnings in the given date range
     * @param startDate start of the date range (inclusive, used when {@code active} is false)
     * @param endDate   end of the date range (inclusive, used when {@code active} is false)
     * @return list of {@link WarningDTO}s matching the query and access level
     * @throws NotFoundException  if the room does not exist
     * @throws ForbiddenException if the user is not permitted to view warnings for this room
     */
    @Override
    public List<WarningDTO> getAllWarningsForRoom(Userx user, UUID roomId,
                                                  boolean active,
                                                  LocalDate startDate,
                                                  LocalDate endDate) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("There's no room with id " + roomId + "."));

        boolean isDeptUser = hasAuthority(user, "CAN_VIEW_OWN_DEPARTMENT_WARNINGS");
        boolean isOfficeUser = hasAuthority(user, "CAN_VIEW_OWN_OFFICE_WARNINGS");
        boolean isBuildingManager = hasAuthority(user, "CAN_VIEW_ALL_ROOMS");
        if (user.getMyRoom() != null) {
            if (isBuildingManager) {
                if (active) {
                    return mapToDTOs(
                            warningsRepository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId)
                    );
                } else {
                    return mapToDTOs(
                            warningsRepository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(
                                    roomId,
                                    startOfDay(startDate),
                                    endOfDay(endDate)
                            )
                    );
                }
            }
            if (isDeptUser) {
                if (!room.getDepartment().getId().equals(user.getMyRoom().getDepartment().getId())) {
                    throw new ForbiddenException("You are not allowed to see others' departments warnings.");
                }

                if (active) {
                    return mapToDTOs(
                            warningsRepository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId)
                    );
                }

                return mapToDTOs(
                        warningsRepository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(
                                roomId,
                                startOfDay(startDate),
                                endOfDay(endDate)
                        )
                );
            }
            // Office-level access: employees see only active warnings for their own room
            // or any room in their department (shared/common rooms shown on the department page)
            if (isOfficeUser) {
                boolean sameRoom = room.equals(user.getMyRoom());
                boolean sameDepartment = room.getDepartment() != null
                        && user.getMyRoom().getDepartment() != null
                        && room.getDepartment().getId().equals(user.getMyRoom().getDepartment().getId())
                        && room.getRoomType().equals(RoomType.SHARED);
                if (sameRoom || sameDepartment) {
                    return mapToDTOs(
                            warningsRepository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId)
                    );
                }
            }
        }
        if (isBuildingManager) {
            if (active) {
                return mapToDTOs(
                        warningsRepository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId)
                );
            } else {
                return mapToDTOs(
                        warningsRepository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(
                                roomId,
                                startOfDay(startDate),
                                endOfDay(endDate)
                        )
                );
            }
        }
        throw new ForbiddenException("You are not allowed to see warnings of this room.");
    }

    /**
     * Creates a new warning from a Raspberry Pi report. The appropriate {@link Tip} is
     * looked up by measurement type and violation direction (OVER/UNDER) and attached
     * if found. The saved warning is pushed to the room's WebSocket topic and the
     * department's topic, and an email notification is sent to opted-in department
     * members.
     *
     * @param dto the warning data reported by the Raspberry Pi
     * @return the saved warning as a {@link WarningDTO}
     * @throws NotFoundException if the room monitoring record cannot be found
     */
    @Override
    @Transactional
    public WarningDTO createWarning(WarningCreateDTO dto) {
        RoomMonitoring room = roomMonitoringRepository.findById(dto.roomId())
                .orElseThrow(() -> new NotFoundException(
                        "RoomMonitoring not found: " + dto.roomId()));

        Warnings warning = warningCreateMapper.mapFrom(dto);
        log.info("Received warning from Raspberry Pi – {}", warning.getMessage());
        switch (warning.getMeasurementType()) {
            case TEMPERATURE -> {
                Tip tip;
                if (warning.getTriggeredValue() > warning.getActiveLimitAtTime()) {
                    tip = tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(warning.getStatus(), ViolationType.OVER, ViolatedSensor.TEMPERATURE).orElse(null);
                } else {
                    tip = tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(warning.getStatus(), ViolationType.UNDER, ViolatedSensor.TEMPERATURE).orElse(null);
                }
                if (tip != null) {
                    warning.setTip(tip);
                    tip.addNewWarning(warning);
                    tipRepository.save(tip);
                }
            }
            case HUMIDITY -> {
                Tip tip;
                if (warning.getTriggeredValue() > warning.getActiveLimitAtTime()) {
                    tip = tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(warning.getStatus(), ViolationType.OVER, ViolatedSensor.HUMIDITY).orElse(null);
                } else {
                    tip = tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(warning.getStatus(), ViolationType.UNDER, ViolatedSensor.HUMIDITY).orElse(null);
                }
                if (tip != null) {
                    warning.setTip(tip);
                    tip.addNewWarning(warning);
                    tipRepository.save(tip);
                }
            }
            default -> {
                Tip tip;
                if (warning.getTriggeredValue() > warning.getActiveLimitAtTime()) {
                    tip = tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(warning.getStatus(), ViolationType.OVER, ViolatedSensor.AIR).orElse(null);
                } else {
                    tip = tipRepository.findByViolationStatusAndViolationTypeAndViolatedSensor(warning.getStatus(), ViolationType.UNDER, ViolatedSensor.AIR).orElse(null);
                }
                if (tip != null) {
                    warning.setTip(tip);
                    tip.addNewWarning(warning);
                    tipRepository.save(tip);
                }
            }
        }
        if (warning.getTip() != null) {
            log.info("Added tip for the warning: {}", warning.getTip().getMsg());
        } else {
            log.info("No tip added for the warning: {} {}", warning.getStatus().name(), warning.getMeasurementType().name());
        }
        warning.setRoomMonitoring(room);
        WarningDTO res = warningMapper.mapTo(warningsRepository.save(warning));
        liveDataService.pushActiveWarning(room.getRoomId(), res);

        Room roomEntity = roomRepository.findById(res.roomId()).get();
        UUID deptId = roomEntity.getDepartment().getId();
        liveDataService.pushActiveWarningDepartment(deptId, res);

        notifyDepartmentByEmail(deptId, res, roomEntity.getRoomNumber());

        return res;
    }

    /**
     * Sends warning email notifications to all department members who have opted in
     * via their {@link at.qe.skeleton.model.UserSettings}.
     *
     * @param deptId     the department UUID
     * @param warning    the warning to include in the email
     * @param roomNumber the room number where the warning was triggered
     */
    private void notifyDepartmentByEmail(UUID deptId, WarningDTO warning, String roomNumber) {
        userxRepository.findAllByDepartment(deptId).forEach(user ->
            userSettingsRepository.findById(user.getId()).ifPresent(settings -> {
                if (settings.isEmailWarnings()
                        && settings.getNotificationEmail() != null
                        && !settings.getNotificationEmail().isBlank()) {
                    String name = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
                    emailService.sendWarningEmail(settings.getNotificationEmail(), name, warning, roomNumber);
                }
            })
        );
    }

    /**
     * Updates the severity and current measured value of an active warning. Used by
     * the Raspberry Pi when conditions worsen or improve while a warning is still open.
     *
     * @param warningId the UUID of the warning to update
     * @param dto       the new status and triggered value
     * @return the updated {@link WarningDTO}
     * @throws NotFoundException  if the warning does not exist
     * @throws ForbiddenException if the warning is already resolved
     */
    @Override
    @Transactional
    public WarningDTO updateWarningStatus(UUID warningId, WarningUpdateStatusDTO dto) {
        Warnings warning = findActiveWarningById(warningId);
        warning.setStatus(dto.status());
        warning.setTriggeredValue(dto.triggeredValue());
        return warningMapper.mapTo(warningsRepository.save(warning));
    }

    /**
     * Resolves a warning and all other active warnings of the same measurement type
     * in the same room. Pushes a resolve event to the room's and department's WebSocket
     * topics for each resolved warning.
     *
     * @param warningId the UUID of the warning to resolve
     * @return the resolved warning as a {@link WarningDTO}
     * @throws NotFoundException   if the warning does not exist
     * @throws ForbiddenException  if the warning is already resolved
     * @throws ValidationException if the warning has no associated room monitoring record
     */
    @Override
    @Transactional
    public WarningDTO resolveWarning(UUID warningId) {
        Warnings warning = findActiveWarningById(warningId);
        log.info("Received resolve active warnings for a room from Raspberry Pi");
        warning.setResolvedAt(LocalDateTime.now());
        AtomicInteger counter = new AtomicInteger(1);
        if (warning.getRoomMonitoring() == null) throw new ValidationException("Warning does not have assigned room. Please contact system administrator.");
        warningsRepository.findAllByRoomAndActiveByType(warning.getRoomMonitoring().getRoomId(), warning.getMeasurementType())
                .forEach(w -> {
                    if (!w.getId().equals(warningId)) {
                        w.setResolvedAt(LocalDateTime.now());
                        warningsRepository.save(w);
                        counter.addAndGet(1);
                    }
                });
        log.info("Resolved {} warnings", counter.get());
        WarningDTO dto = warningMapper.mapTo(warningsRepository.save(warning));
        liveDataService.resolveActiveWarning(dto.roomId(), dto);
        Room room = roomRepository.findById(dto.roomId()).orElse(null);
        if (room != null) {
            UUID deptId = room.getDepartment().getId();
            for (int i = 0; i < counter.get(); i++) {
                liveDataService.resolveActiveWarningDepartment(deptId, dto);
            }
        }
        return dto;
    }

    /**
     * Returns a summary-level violation log for all rooms in the given department.
     * When {@code active} is {@code true}, returns only unresolved warnings regardless
     * of date range. Otherwise, returns all warnings created within the given date range.
     *
     * @param id        the department UUID
     * @param active    if {@code true}, returns only unresolved warnings
     * @param startDate start of the date range (used when {@code active} is false)
     * @param endDate   end of the date range (used when {@code active} is false)
     * @return list of {@link SummaryWarningDTO}s
     * @throws NotFoundException if the department does not exist
     */
    @Override
    public List<SummaryWarningDTO> getViolationLogForDepartment(UUID id, boolean active, LocalDate startDate, LocalDate endDate) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department with id " + id + " was not found."));
        List<Warnings> summary = new ArrayList<>();
        if (active) {
            List<UUID> roomIds = department.getRooms().stream().map(Room::getId).toList();
            summary = warningsRepository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNull(roomIds);
        } else {
            for (Room room : department.getRooms()) {
                summary.addAll(warningsRepository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(room.getId(),
                        startOfDay(startDate),
                        endOfDay(endDate)));
            }
        }
        return summary.stream()
                .map(warning -> new SummaryWarningDTO(
                        warning.getId(),
                        warning.getMeasurementType(),
                        warning.getStatus(),
                        warning.getMessage(),
                        warning.getTriggeredValue(),
                        warning.getActiveLimitAtTime(),
                        warning.getCreatedAt(),
                        warning.getResolvedAt(),
                        warning.isActive()
                )).toList();
    }

    /**
     * Returns a detailed warning log for all rooms in the given department. The
     * authenticated user must belong to the requested department. When {@code active}
     * is {@code true}, only unresolved warnings within the date range are returned.
     *
     * @param user      the authenticated user making the request
     * @param id        the department UUID
     * @param active    if {@code true}, returns only unresolved warnings in the range
     * @param startDate start of the date range (inclusive)
     * @param endDate   end of the date range (inclusive)
     * @return list of {@link WarningDTO}s
     * @throws NotFoundException  if the department does not exist
     * @throws ForbiddenException if the user does not belong to the requested department
     */
    @Override
    public List<WarningDTO> getDetailedViolationLogForDepartment(Userx user, UUID id,
                                                                 boolean active,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department with id " + id + " was not found."));

        if (user.getMyRoom() == null || !department.getId().equals(user.getMyRoom().getDepartment().getId())) {
            throw new ForbiddenException("You're not allowed to see other department's warnings");
        }

        List<UUID> roomIds = department.getRooms()
                .stream()
                .map(Room::getId)
                .toList();

        List<Warnings> warnings;

        if (active) {
            warnings = warningsRepository
                    .findByRoomMonitoring_RoomIdInAndResolvedAtIsNullAndCreatedAtBetween(
                            roomIds,
                            startOfDay(startDate),
                            endOfDay(endDate)
                    );
        } else {
            warnings = warningsRepository
                    .findByRoomMonitoring_RoomIdInAndCreatedAtBetween(
                            roomIds,
                            startOfDay(startDate),
                            endOfDay(endDate)
                    );
        }

        return mapToDTOs(warnings);
    }

    /**
     * Returns the total count of currently active (unresolved) warnings across all
     * rooms in the given building.
     *
     * @param id the building UUID
     * @return an {@link ActiveViolationBuildingStats} containing the active warning count
     * @throws NotFoundException if the building does not exist
     */
    @Override
    public ActiveViolationBuildingStats getActiveViolationsForBuilding(UUID id) {
        Building building = buildingRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Building with ID %s was not found.".formatted(id.toString()))
        );
        List<UUID> roomIds = new ArrayList<>();
        for (Department department : building.getDepartments()) {
            department.getRooms().forEach(room -> roomIds.add(room.getId()));
        }
        return new ActiveViolationBuildingStats(warningsRepository.findByRoomMonitoring_RoomIdInAndResolvedAtIsNull(roomIds).size());
    }

    /**
     * Looks up an active (unresolved) warning by ID.
     *
     * @param warningId the warning UUID
     * @return the active {@link Warnings} entity
     * @throws NotFoundException  if no warning with that ID exists
     * @throws ForbiddenException if the warning is already resolved
     */
    private Warnings findActiveWarningById(UUID warningId) {
        Warnings warning = warningsRepository.findById(warningId)
                .orElseThrow(() -> new NotFoundException(
                        "Warning not found: " + warningId));

        if (!warning.isActive()) {
            throw new ForbiddenException(
                    "Warning " + warningId + " is already resolved");
        }
        return warning;
    }

    /**
     * Returns {@code true} if the given user holds the specified authority.
     *
     * @param user      the user to check
     * @param authority the authority string to look for
     * @return {@code true} if the authority is present
     */
    private boolean hasAuthority(Userx user, String authority) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }

    /**
     * Returns a {@link LocalDateTime} representing the start of the given date
     * (00:00:00.000).
     *
     * @param date the date
     * @return midnight at the start of the day
     */
    private LocalDateTime startOfDay(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MIN);
    }

    /**
     * Returns a {@link LocalDateTime} representing the end of the given date
     * (23:59:59.999...).
     *
     * @param date the date
     * @return the last instant of the day
     */
    private LocalDateTime endOfDay(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MAX);
    }

    /**
     * Maps a list of {@link Warnings} entities to {@link WarningDTO}s.
     *
     * @param warnings the entities to map
     * @return list of DTOs
     */
    private List<WarningDTO> mapToDTOs(List<Warnings> warnings) {
        return warnings.stream()
                .map(warningMapper::mapTo)
                .toList();
    }
}