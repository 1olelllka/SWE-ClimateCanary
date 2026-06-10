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

    // get warnings for a specific room
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
            // Building-level access (if building manager happens to have a room)
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
            // Department-level access
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

    //  for Pi to report a new violation
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

    // for Pi to update severity while warning is still active                   //
    @Override
    @Transactional
    public WarningDTO updateWarningStatus(UUID warningId, WarningUpdateStatusDTO dto) {
        Warnings warning = findActiveWarningById(warningId);
        warning.setStatus(dto.status());
        warning.setTriggeredValue(dto.triggeredValue());
        return warningMapper.mapTo(warningsRepository.save(warning));
    }

    // for Pi to resolve warning
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

    private boolean hasAuthority(Userx user, String authority) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }

    private LocalDateTime startOfDay(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MIN);
    }

    private LocalDateTime endOfDay(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MAX);
    }

    private List<WarningDTO> mapToDTOs(List<Warnings> warnings) {
        return warnings.stream()
                .map(warningMapper::mapTo)
                .toList();
    }
}
