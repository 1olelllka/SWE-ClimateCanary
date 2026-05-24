package at.qe.skeleton.services.impl;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.OccupancyDTO;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.ForbiddenException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.AbsenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
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
        Optional<Userx> manager = userxRepository.findById(absence.getAssignedTo());
        if (manager.isEmpty()) {
            throw new NotFoundException("Manager with id %s was not found".formatted(absence.getAssignedTo()));
        }

        Set<String> authorities = manager.get().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (!authorities.contains("CAN_MANAGE_ABSENCES")) {
            throw new ForbiddenException("Assigned person does not have manager rights.");
        }

        UUID id = absence.getUser().getId();
        Userx user = userxRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id %s was not found".formatted(id)));
        if (absence.getTypeOfAbsence().equals(AbsenceType.VACATION) && user.getNumberOfAbsences() < ChronoUnit.DAYS.between(absence.getStartDate().toLocalDate(), absence.getEndDate().toLocalDate()))
            throw new ValidationException("You don't have enough amount of absences available.");

        Userx managerUser = manager.get();

        if (user.getMyRoom() == null || user.getMyRoom().getDepartment() == null) {
            throw new ValidationException("User has no assigned room or department.");
        }

        if (managerUser.getMyRoom() == null || managerUser.getMyRoom().getDepartment() == null) {
            throw new ValidationException("Manager has no assigned room or department.");
        }

        if (!user.getMyRoom().getDepartment().getId()
                .equals(managerUser.getMyRoom().getDepartment().getId())) {
            throw new ForbiddenException("You cannot apply for absence to this manager.");
        }

        absence.setStatus(AbsenceStatus.PENDING);
        if (absence.getTypeOfAbsence().equals(AbsenceType.VACATION)) {
            user.setNumberOfAbsences((int) (user.getNumberOfAbsences() - ChronoUnit.DAYS.between(absence.getStartDate().toLocalDate(), absence.getEndDate().toLocalDate())));
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
        if (user.getMyRoom() != null && manager.get().getMyRoom() != null && !user.getMyRoom().getDepartment().getId().equals(manager.get().getMyRoom().getDepartment().getId())) {
            throw new ForbiddenException("You cannot update absence status for this employee.");
        }
        absence.setStatus(status);
        if (status == AbsenceStatus.REJECTED && absence.getTypeOfAbsence().equals(AbsenceType.VACATION)) {
            user.setNumberOfAbsences((int) (user.getNumberOfAbsences() + ChronoUnit.DAYS.between(absence.getStartDate().toLocalDate(), absence.getEndDate().toLocalDate())));
            userxRepository.save(user);
        }
        return absenceRepository.save(absence);
    }

    @Override
    @Transactional
    public Absence cancelAbsence(UUID absenceId, Userx user) {
        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new NotFoundException("Absence with id " + absenceId + " was not found."));
        if (!absence.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not allowed to cancel this absence.");
        }
        if (absence.getStatus() != AbsenceStatus.PENDING) {
            throw new ValidationException("Only pending absences can be cancelled.");
        }
        absence.setStatus(AbsenceStatus.CANCELLED);
        user.setNumberOfAbsences((int) (user.getNumberOfAbsences() + ChronoUnit.DAYS.between(absence.getStartDate().toLocalDate(), absence.getEndDate().toLocalDate())));
        userxRepository.save(user);
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

    @Override
    public boolean isClockedIn(Userx user) {
        return clockStatusRepository.findById(user.getId().toString())
                .map(UserClockStatus::isClockedIn)
                .orElse(false);
    }

    @Override
    @Transactional
    public List<Userx> getAllAvailableManagersForUser(Userx user) {
        return userxRepository.findAllByDepartment(user.getMyRoom().getDepartment().getId())
                .stream()
                .filter(u -> u.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch("CAN_MANAGE_ABSENCES"::equals))
                .toList();
    }

}
