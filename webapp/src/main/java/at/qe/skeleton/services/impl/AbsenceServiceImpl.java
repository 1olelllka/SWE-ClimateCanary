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
import at.qe.skeleton.services.EmailService;
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

/**
 * Implementation of {@link AbsenceService} handling absence lifecycle management,
 * clock-in/clock-out operations, and related room occupancy tracking.
 *
 * <p>Absence rules enforced here:
 * <ul>
 *   <li>The assigned manager must hold the {@code CAN_MANAGE_ABSENCES} authority.</li>
 *   <li>Employee and manager must belong to the same department.</li>
 *   <li>Vacation absences are deducted from the user's allowance on creation and
 *       refunded on rejection or cancellation.</li>
 * </ul>
 */
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
    private final UserSettingsRepository userSettingsRepository;
    private final EmailService emailService;

    /**
     * Returns a paginated list of all absences for the given user.
     *
     * @param id       the user's UUID
     * @param pageable pagination parameters
     * @return page of absences belonging to the user
     */
    @Override
    public Page<Absence> getAllAbsencesById(UUID id, Pageable pageable) {
        return absenceRepository.findAllByUserId(id, pageable);
    }

    /**
     * Creates and persists a new absence request for a user.
     * Validates that the assigned manager exists, has the required authority,
     * belongs to the same department as the user, and — for vacation absences —
     * that the user has sufficient remaining allowance. Deducts vacation days
     * from the user's balance on success.
     *
     * @param absence the absence to create (status will be set to {@link AbsenceStatus#PENDING})
     * @return the saved {@link Absence}
     * @throws NotFoundException   if the manager or user cannot be found
     * @throws ForbiddenException  if the manager lacks the required authority or belongs
     *                             to a different department
     * @throws ValidationException if the user has insufficient vacation days, or if
     *                             the user or manager has no assigned room/department
     */
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

    /**
     * Returns the absence with the given ID, verifying it was assigned to the
     * provided manager.
     *
     * @param id      the absence UUID
     * @param manager the manager requesting the absence
     * @return the matching {@link Absence}
     * @throws NotFoundException  if no absence with that ID exists
     * @throws ForbiddenException if the absence was not assigned to the given manager
     */
    @Override
    public Absence getAbsenceById(UUID id, Userx manager) {
        Absence absence = absenceRepository.findById(id).orElseThrow(() -> new NotFoundException("Absence with id " + id + " was not found."));
        if (absence.getAssignedTo().equals(manager.getId())) {
            return absence;
        }
        throw new ForbiddenException("This absence was not assigned to you.");
    }

    /**
     * Updates the status of an existing absence. If the new status is
     * {@link AbsenceStatus#REJECTED} and the absence type is vacation, the deducted
     * days are refunded to the user's balance. Sends an email notification if the
     * user has enabled absence email alerts in their settings.
     *
     * @param id     the absence UUID
     * @param status the new status to apply
     * @return the updated {@link Absence}
     * @throws NotFoundException  if the absence or the assigned manager cannot be found
     * @throws ForbiddenException if the manager lacks the required authority or belongs
     *                            to a different department than the user
     */
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
        Absence saved = absenceRepository.save(absence);

        userSettingsRepository.findById(user.getId()).ifPresent(settings -> {
            if (settings.isEmailAbsences()
                    && settings.getNotificationEmail() != null
                    && !settings.getNotificationEmail().isBlank()) {
                String name = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
                emailService.sendAbsenceStatusEmail(settings.getNotificationEmail(), name, saved);
            }
        });

        return saved;
    }

    /**
     * Cancels a pending absence on behalf of the owning user. Only the user who owns
     * the absence may cancel it, and only while it is in {@link AbsenceStatus#PENDING}
     * state. Refunds vacation days if applicable.
     *
     * @param absenceId the absence UUID
     * @param user      the user requesting cancellation
     * @return the cancelled {@link Absence}
     * @throws NotFoundException   if the absence does not exist
     * @throws ForbiddenException  if the requesting user does not own the absence
     * @throws ValidationException if the absence is not in {@code PENDING} state
     */
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
        if (absence.getTypeOfAbsence().equals(AbsenceType.VACATION))
            user.setNumberOfAbsences((int) (user.getNumberOfAbsences() + ChronoUnit.DAYS.between(absence.getStartDate().toLocalDate(), absence.getEndDate().toLocalDate())));
        absence.setStatus(AbsenceStatus.CANCELLED);
        userxRepository.save(user);
        return absenceRepository.save(absence);
    }

    /**
     * Returns a paginated list of absences assigned to the given manager.
     *
     * @param user     the manager
     * @param pageable pagination parameters
     * @return page of absences assigned to the manager
     */
    @Override
    public Page<Absence> getAllAbsencesByDepartment(Userx user, Pageable pageable) {
        return absenceRepository.findByAssignedTo(user.getId(), pageable);
    }

    /**
     * Records a clock-in event for the user. If the user has an assigned room,
     * increments the room's occupancy counter and notifies the room's Raspberry Pi
     * (if present) via an {@link at.qe.skeleton.commands.NotifyRaspberryCommand} event.
     *
     * @param user the user clocking in
     * @throws ConflictException if the user is already clocked in
     * @throws NotFoundException if the user's assigned room does not exist
     */
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

    /**
     * Records a clock-out event for the user. If the user has an assigned room,
     * decrements the room's occupancy counter (floor at zero) and notifies the
     * room's Raspberry Pi (if present).
     *
     * @param user the user clocking out
     * @throws ConflictException if the user has never clocked in or is already clocked out
     * @throws NotFoundException if the user's assigned room does not exist
     */
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

    /**
     * Returns whether the given user is currently clocked in.
     *
     * @param user the user to check
     * @return {@code true} if clocked in, {@code false} otherwise (including if no
     *         clock record exists)
     */
    @Override
    public boolean isClockedIn(Userx user) {
        return clockStatusRepository.findById(user.getId().toString())
                .map(UserClockStatus::isClockedIn)
                .orElse(false);
    }

    /**
     * Returns all managers in the same department as the given user who hold the
     * {@code CAN_MANAGE_ABSENCES} authority.
     *
     * @param user the employee whose department is used to filter managers
     * @return list of eligible managers
     */
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