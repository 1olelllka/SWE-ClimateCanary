package at.qe.skeleton.services.impl;

import at.qe.skeleton.dtos.WarningCreateDTO;
import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.dtos.WarningUpdateStatusDTO;
import at.qe.skeleton.mappers.WarningCreateMapper;
import at.qe.skeleton.mappers.WarningMapper;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.WarningStatus;
import at.qe.skeleton.model.Warnings;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.WarningRepository;
import at.qe.skeleton.services.WarningService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WarningServiceImpl implements WarningService {

    private final WarningRepository warningsRepository;
    private final RoomMonitoringRepository roomMonitoringRepository;
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
    public List<WarningDTO> getActiveWarningsForRoom(UUID roomId) {
        return warningsRepository
                .findByRoomMonitoring_RoomIdAndResolvedAtIsNull(roomId)
                .stream()
                .map(warningMapper::mapTo)
                .toList();
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
    public List<WarningDTO> getViolationLog(UUID roomId) {
        return warningsRepository
                .findByRoomMonitoring_RoomId(roomId)
                .stream()
                .map(warningMapper::mapTo)
                .toList();
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
