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
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.repositories.WarningRepository;
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
import java.util.stream.Collectors;

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

    // get all active warnings
    @Override
    public List<WarningDTO> getAllActiveWarnings() {
        return warningsRepository.findAllActive()
                .stream()
                .map(warningMapper::mapTo)
                .toList();
    }

    // get warnings for a specific room
    @Override
    public List<WarningDTO> getAllWarningsForRoom(Userx authenticated, UUID roomId, Boolean active, LocalDate startDate, LocalDate endDate) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("There's no room with id " + roomId + "."));
        if (authenticated.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("CAN_VIEW_OWN_DEPARTMENT_WARNINGS"))) {
            if (room.getDepartment().equals(authenticated.getMyRoom().getDepartment())) {
                if (active) {
                    return warningsRepository
                            .findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId)
                            .stream()
                            .map(warningMapper::mapTo)
                            .toList();
                } else {
                    return warningsRepository
                            .findByRoomMonitoring_RoomIdAndCreatedAtBetween(roomId,
                                    LocalDateTime.of(startDate, LocalTime.of(0, 0)),
                                    LocalDateTime.of(endDate, LocalTime.of(0, 0)))
                            .stream()
                            .map(warningMapper::mapTo)
                            .toList();
                }
            }
            throw new ForbiddenException("You are not allowed to see others' departments warnings.");
        } else if (authenticated.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("CAN_VIEW_OWN_OFFICE_WARNINGS"))) {
            if (room.equals(authenticated.getMyRoom())) {
                return warningsRepository
                        .findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId)
                        .stream()
                        .map(warningMapper::mapTo)
                        .toList();
            }
        }
        throw new ForbiddenException("You are not allowed to see warnings of this room.");
    }

    //  for Pi to report a new violation
    @Override
    @Transactional
    public WarningDTO createWarning(WarningCreateDTO dto) {
        RoomMonitoring room = roomMonitoringRepository.findById(dto.roomId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "RoomMonitoring not found: " + dto.roomId()));

        Warnings warning = warningCreateMapper.mapFrom(dto);
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

    // full history of active and resolved warning for a specific room
    @Override
    public List<WarningDTO> getViolationLog(Userx authenticated, UUID roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
        if (room.getDepartment().equals(authenticated.getMyRoom().getDepartment())) {
            return warningsRepository
                    .findByRoomMonitoring_RoomId(roomId)
                    .stream()
                    .map(warningMapper::mapTo)
                    .toList();
        }
        throw new ForbiddenException("You are not allowed to see the violation log for this room.");
    }

    @Override
    public List<SummaryWarningDTO> getViolationLogForDepartment(UUID id, Boolean active, LocalDate startDate, LocalDate endDate) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department with id " + id + " was not found."));
        List<Warnings> summary = new ArrayList<>();
        if (active) {
            List<Warnings> activeWarnings = warningsRepository.findAllActive();
            List<UUID> roomIds = department.getRooms().stream().map(room -> room.getId()).toList();
            for (Warnings warnings : activeWarnings) {
                if (roomIds.contains(warnings.getRoomMonitoring().getRoomId())) {
                    summary.add(warnings);
                }
            }
        } else {
            for (Room room : department.getRooms()) {
                summary.addAll(warningsRepository.findByRoomMonitoring_RoomIdAndCreatedAtBetween(room.getId(),
                        LocalDateTime.of(startDate, LocalTime.of(0, 0)),
                        LocalDateTime.of(endDate, LocalTime.of(0, 0))));
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
                )).collect(Collectors.toList());
    }

    @Override
    public List<WarningDTO> getDetailedViolationLogForDepartment(Userx user, UUID id, Boolean active, LocalDate startDate, LocalDate endDate) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department with id " + id + " was not found."));
        List<WarningDTO> summary = new ArrayList<>();
        for (Room room : department.getRooms()) {
            summary.addAll(getAllWarningsForRoom(user, room.getId(), active, startDate, endDate));
        }
        return summary;
    }

    private Warnings findActiveWarningById(UUID warningId) {
        Warnings warning = warningsRepository.findById(warningId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Warning not found: " + warningId));

        if (!warning.isActive()) {
            throw new IllegalStateException(
                    "Warning " + warningId + " is already resolved");
        }
        return warning;
    }
}
