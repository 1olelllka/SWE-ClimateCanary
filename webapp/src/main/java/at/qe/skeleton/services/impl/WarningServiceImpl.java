package at.qe.skeleton.services.impl;

import at.qe.skeleton.dtos.SummaryWarningDTO;
import at.qe.skeleton.dtos.WarningCreateDTO;
import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.dtos.WarningUpdateStatusDTO;
import at.qe.skeleton.exceptions.ForbiddenException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.mappers.WarningCreateMapper;
import at.qe.skeleton.mappers.WarningMapper;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.WarningService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WarningServiceImpl implements WarningService {

    private final WarningRepository warningsRepository;
    private final RoomMonitoringRepository roomMonitoringRepository;
    private final RoomRepository roomRepository;
    private final DepartmentRepository departmentRepository;
    private final WarningMapper warningMapper;
    private final WarningCreateMapper warningCreateMapper;
    private final TipRepository tipRepository;


    // get warnings for a specific room
    @Override
    public List<WarningDTO> getAllWarningsForRoom(Userx user, UUID roomId,
                                                  Boolean active,
                                                  LocalDate startDate,
                                                  LocalDate endDate) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("There's no room with id " + roomId + "."));

        boolean isDeptUser = hasAuthority(user, "CAN_VIEW_OWN_DEPARTMENT_WARNINGS");
        boolean isOfficeUser = hasAuthority(user, "CAN_VIEW_OWN_OFFICE_WARNINGS");
        if (user.getMyRoom() != null) {
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
            // Office-level access
            if (isOfficeUser && room.equals(user.getMyRoom()) && active) {
                return mapToDTOs(
                        warningsRepository.findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId)
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
        switch (warning.getMeasurementType()) {
            case TEMPERATURE -> {
                Tip tip;
                if (warning.getTriggeredValue() > room.getTempLimit().getMaxVal()) {
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
                if (warning.getTriggeredValue() > room.getHumLimit().getMaxVal()) {
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
                if (warning.getTriggeredValue() > room.getPolLimit().getMaxVal()) {
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
        warning.setRoomMonitoring(room);
        return warningMapper.mapTo(warningsRepository.save(warning));
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
        warning.setResolvedAt(LocalDateTime.now());
        warning.setStatus(WarningStatus.GREEN);
        return warningMapper.mapTo(warningsRepository.save(warning));
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

        if (!department.getId().equals(user.getMyRoom().getDepartment().getId())) {
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
