package at.qe.skeleton.services.impl;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.OccupancyDTO;
import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import at.qe.skeleton.dtos.UpdateType;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.ForbiddenException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.AbsenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AbsenceServiceImpl implements AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final UserxRepository userxRepository;
    private final RoomOccupancyRepository roomOccupancyRepository;
    private final RoomRepository roomRepository;
    private final RoomMonitoringRepository roomMonitoringRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationClient notificationClient;
    private final UserClockStatusRepository clockStatusRepository;

    @Override
    public Page<Absence> getAllAbsencesById(UUID id, Pageable pageable) {
        return absenceRepository.findAllByUserId(id, pageable);
    }

    @Override
    @Transactional
    public Absence createNewAbsenceForUser(Absence absence) {
        if (absence.getAssignedTo().equals(absence.getUser().getId())) {
            throw new ValidationException("Assigned person must not be the same as you.");
        }
        Optional<Userx> manager = userxRepository.findById(absence.getAssignedTo());
        if (manager.isEmpty()) {
            throw new NotFoundException("Manager with id " + absence.getAssignedTo() + " was not found.");
        }
        Set<String> authorities = manager.get().getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        if (!authorities.contains("CAN_MANAGE_ABSENCES")) {
            throw new ForbiddenException("Assigned person does not have manager rights.");
        }
        UUID id = absence.getUser().getId();
        Userx user = userxRepository.findById(id).orElseThrow(() -> new NotFoundException("User with id " + id + " was not found"));
        if (!user.getMyRoom().getDepartment().getId().equals(manager.get().getMyRoom().getDepartment().getId())) {
            throw new ForbiddenException("You cannot apply for absence to this manager.");
        }
        return absenceRepository.save(absence);
    }

    @Override
    public Absence getAbsenceById(UUID id, Userx manager) {
        Absence absence = absenceRepository.findById(id).orElseThrow(() -> new NotFoundException("Absence with id " + id + " was not found."));
        if (absence.getAssignedTo().equals(manager.getId())) {
            return absence;
        }
        throw new ForbiddenException("This absence was not assigned to you.");
    }

    @Override
    public void deleteAbsenceById(UUID id, Userx user) {
        Absence absence = absenceRepository.findById(id).orElseThrow(() -> new NotFoundException("Absence with id " + id + " was not found."));
        if (user.getId().equals(absence.getUser().getId())) {
            absenceRepository.deleteById(id);
            return;
        }
        throw new ForbiddenException("You are not allowed to delete this absence.");
    }

    @Override
    @Transactional
    public Absence updateAbsenceStatus(UUID id, AbsenceStatus status) {
        Absence absence = absenceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Absence with id " + id + " was not found."));
        Optional<Userx> manager = userxRepository.findById(absence.getAssignedTo());
        if (manager.isEmpty()) {
            throw new NotFoundException("Manager with id " + absence.getAssignedTo() + " was not found.");
        }
        Set<String> authorities = manager.get().getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        if (!authorities.contains("CAN_MANAGE_ABSENCES")) {
            throw new ForbiddenException("Assigned person does not have manager rights.");
        }
        UUID userId = absence.getUser().getId();
        Userx user = userxRepository.findById(userId).orElseThrow(() -> new NotFoundException("User with id " + userId + " was not found"));
        if (!user.getMyRoom().getDepartment().getId().equals(manager.get().getMyRoom().getDepartment().getId())) {
            throw new ForbiddenException("You cannot update absence status for this employee.");
        }
        absence.setStatus(status);
        return absenceRepository.save(absence);
    }

    @Override
    public Page<Absence> getAllAbsencesByDepartment(Userx user, Pageable pageable) {
        return absenceRepository.findByAssignedTo(user.getId(), pageable);
    }

    @Override
    @Transactional
    public void clockIn(Userx user) {
        UserClockStatus status = clockStatusRepository.findById(user.getId().toString()).orElse(UserClockStatus.builder().userId(user.getId()).clockedIn(false).build());
        if (status.isClockedIn()) throw new ConflictException("You cannot clock in twice.");
        if (user.getMyRoom() != null) {
            if (!roomRepository.existsById(user.getMyRoom().getId())) throw new NotFoundException("Room with id " + user.getMyRoom().getId() + " was not found.");
            RoomMonitoring monitoring = roomMonitoringRepository.findById(user.getMyRoom().getId()).get(); // monitoring should exist
            RoomOccupancy room = roomOccupancyRepository.findById(user.getMyRoom().getId().toString())
                    .orElse(RoomOccupancy.builder().peopleCnt(0).roomId(user.getMyRoom().getId()).build());
            room.setPeopleCnt(room.getPeopleCnt() + 1);
            roomOccupancyRepository.save(room);
            if (monitoring.getRaspberryPi() != null) {
                eventPublisher.publishEvent(new NotifyRaspberryCommand(
                        new OccupancyDTO(room.getPeopleCnt(), room.getRoomId(), room.getPeopleCnt() < 5),
                        monitoring.getRaspberryPi(),
                        notificationClient
                ));
            }
        }
        status.setClockedIn(true);
        clockStatusRepository.save(status);
    }

    @Override
    @Transactional
    public void clockOut(Userx user) {
        UserClockStatus status = clockStatusRepository.findById(user.getId().toString()).orElseThrow(() -> new ConflictException("You cannot clock out without clocking in."));
        if (!status.isClockedIn()) throw new ConflictException("You cannot clock out twice.");
        if (user.getMyRoom() != null) {
            if (!roomRepository.existsById(user.getMyRoom().getId())) throw new NotFoundException("Room with id " + user.getMyRoom().getId() + " was not found.");
            RoomMonitoring monitoring = roomMonitoringRepository.findById(user.getMyRoom().getId()).get(); // monitoring should exist
            RoomOccupancy room = roomOccupancyRepository.findById(user.getMyRoom().getId().toString())
                    .orElse(RoomOccupancy.builder().roomId(user.getMyRoom().getId()).peopleCnt(0).build());
            if (room.getPeopleCnt() > 0) {
                room.setPeopleCnt(room.getPeopleCnt() - 1);
            }
            roomOccupancyRepository.save(room);
            if (monitoring.getRaspberryPi() != null) {
                eventPublisher.publishEvent(new NotifyRaspberryCommand(
                        new OccupancyDTO(room.getPeopleCnt(), room.getRoomId(), room.getPeopleCnt() < 5),
                        monitoring.getRaspberryPi(),
                        notificationClient
                ));
            }
        }
        status.setClockedIn(false);
        clockStatusRepository.save(status);
    }
}
