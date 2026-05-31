package at.qe.skeleton.tests.services;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.ForbiddenException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.EmailService;
import at.qe.skeleton.services.impl.AbsenceServiceImpl;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceUnitTests {

    @Mock private AbsenceRepository absenceRepository;
    @Mock private UserxRepository userxRepository;
    @Mock private RoomOccupancyRepository roomOccupancyRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private RoomMonitoringRepository roomMonitoringRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private NotificationClient notificationClient;
    @Mock private UserClockStatusRepository clockStatusRepository;
    @Mock private UserSettingsRepository userSettingsRepository;
    @Mock private EmailService emailService;
    @InjectMocks private AbsenceServiceImpl absenceService;

    private Userx user;
    private Userx manager;
    private Absence absence;
    private Department dept;
    private Room room;
    private RoomMonitoring monitoringWithPi;
    private RoomMonitoring monitoringWithoutPi;

    @BeforeEach
    void setUp() {
        dept = TestDataUtil.createDepartmentEntity(TestDataUtil.createBuildingEntity());
        dept.setId(UUID.randomUUID());

        room = TestDataUtil.createRoomEntity(dept);
        room.setId(UUID.randomUUID());

        user = TestDataUtil.createUserxEntity(null, room);
        user.setId(UUID.randomUUID());

        manager = TestDataUtil.createUserxEntity(null, room);
        manager.setId(UUID.randomUUID());
        manager.setUserRoles(Set.of(UserRole.builder()
                .permissions(Set.of(Permission.CAN_MANAGE_ABSENCES)).build()));

        absence = TestDataUtil.createAbsence(user);
        this.user.setNumberOfAbsences((int) (25 - ChronoUnit.DAYS.between(absence.getStartDate().toLocalDate(), absence.getEndDate().toLocalDate())));
        absence.setId(UUID.randomUUID());
        absence.setAssignedTo(manager.getId());

        RaspberryPi raspberry = RaspberryPi.builder().id(UUID.randomUUID()).port(8000).ip("123.123.12.32").build();

        monitoringWithPi = RoomMonitoring.builder()
                .raspberryPi(raspberry)
                .build();

        monitoringWithoutPi = RoomMonitoring.builder()
                .build();
    }

    @Test
    void testThatGetAllAbsencesByIdDelegatesToRepository() {
        Pageable pageable = Pageable.unpaged();
        Page<Absence> expected = new PageImpl<>(List.of(absence));
        when(absenceRepository.findAllByUserId(user.getId(), pageable)).thenReturn(expected);

        Page<Absence> result = absenceService.getAllAbsencesById(user.getId(), pageable);

        assertEquals(expected, result);
        verify(absenceRepository).findAllByUserId(user.getId(), pageable);
    }

    @Test
    void testThatGetAllAbsencesByDepartmentDelegatesToRepository() {
        Pageable pageable = Pageable.unpaged();
        Page<Absence> expected = new PageImpl<>(List.of(absence));
        when(absenceRepository.findByAssignedTo(manager.getId(), pageable)).thenReturn(expected);

        Page<Absence> result = absenceService.getAllAbsencesByDepartment(manager, pageable);

        assertEquals(expected, result);
        verify(absenceRepository).findByAssignedTo(manager.getId(), pageable);
    }

// Replace all three createNewAbsence tests with these five:

    @Test
    void testThatCreateNewAbsenceThrowsNotFoundWhenManagerDoesNotExist() {
        when(userxRepository.findById(manager.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> absenceService.createNewAbsenceForUser(absence));
        verify(absenceRepository, never()).save(any());
    }

    @Test
    void testThatCreateNewAbsenceThrowsForbiddenWhenManagerLacksRights() {
        manager.setUserRoles(Collections.emptySet());
        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

        assertThrows(ForbiddenException.class, () -> absenceService.createNewAbsenceForUser(absence));
        verify(absenceRepository, never()).save(any());
    }

    @Test
    void testThatCreateNewAbsenceThrowsNotFoundWhenUserDoesNotExist() {
        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userxRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> absenceService.createNewAbsenceForUser(absence));
        verify(absenceRepository, never()).save(any());
    }

    @Test
    void testThatCreateNewAbsenceThrowsValidationExceptionWhenNotEnoughVacationDaysLeft() {
        // user.numberOfAbsences is already set to (25 - absenceDays) in setUp,
        // so set it to 0 to guarantee insufficient days for the VACATION absence
        user.setNumberOfAbsences(0);
        absence.setTypeOfAbsence(AbsenceType.VACATION);

        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userxRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(ValidationException.class, () -> absenceService.createNewAbsenceForUser(absence));
        verify(absenceRepository, never()).save(any());
    }

    @Test
    void testThatCreateNewAbsenceThrowsForbiddenWhenManagerIsInDifferentDepartment() {
        Department otherDept = TestDataUtil.createDepartmentEntity(TestDataUtil.createBuildingEntity());
        otherDept.setId(UUID.randomUUID());
        manager.setMyRoom(TestDataUtil.createRoomEntity(otherDept));

        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userxRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class, () -> absenceService.createNewAbsenceForUser(absence));
        verify(absenceRepository, never()).save(any());
    }

    @Test
    void testThatCreateNewAbsenceSucceedsAndSetsStatusToPending() {
        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userxRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(absenceRepository.save(any(Absence.class))).thenReturn(absence);

        Absence result = absenceService.createNewAbsenceForUser(absence);

        assertNotNull(result);
        assertEquals(AbsenceStatus.PENDING, absence.getStatus());
        verify(absenceRepository).save(absence);
    }

    @Test
    void testThatCreateNewAbsenceDeductsVacationDaysWhenValid() {
        int initialDays = user.getNumberOfAbsences();
        long absenceDays = ChronoUnit.DAYS.between(
                absence.getStartDate().toLocalDate(), absence.getEndDate().toLocalDate());
        absence.setTypeOfAbsence(AbsenceType.VACATION);

        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userxRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(absenceRepository.save(any(Absence.class))).thenReturn(absence);

        absenceService.createNewAbsenceForUser(absence);

        assertEquals((int) (initialDays - absenceDays), user.getNumberOfAbsences());
    }

    @Test
    void testThatCreateNewAbsenceDoesNotDeductDaysForNonVacationType() {
        int initialDays = user.getNumberOfAbsences();
        absence.setTypeOfAbsence(AbsenceType.ILLNESS);

        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userxRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(absenceRepository.save(any(Absence.class))).thenReturn(absence);

        absenceService.createNewAbsenceForUser(absence);

        assertEquals(initialDays, user.getNumberOfAbsences());
    }


    @Test
    void testThatGetAbsenceByIdReturnsAbsenceForCorrectManager() {
        when(absenceRepository.findById(absence.getId())).thenReturn(Optional.of(absence));

        Absence result = absenceService.getAbsenceById(absence.getId(), manager);

        assertEquals(absence, result);
    }

    @Test
    void testThatGetAbsenceByIdThrowsNotFoundWhenAbsenceDoesNotExist() {
        when(absenceRepository.findById(absence.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> absenceService.getAbsenceById(absence.getId(), manager));
    }

    @Test
    void testThatGetAbsenceByIdThrowsForbiddenForWrongManager() {
        Userx wrongManager = new Userx();
        wrongManager.setId(UUID.randomUUID());
        when(absenceRepository.findById(absence.getId())).thenReturn(Optional.of(absence));

        assertThrows(ForbiddenException.class, () -> absenceService.getAbsenceById(absence.getId(), wrongManager));
    }

    @Test
    void testThatUpdateAbsenceStatusChangesStatusSuccessfullyAndUsersAvailableDays() {
        when(absenceRepository.findById(absence.getId())).thenReturn(Optional.of(absence));
        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userxRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(absenceRepository.save(any(Absence.class))).thenAnswer(i -> i.getArgument(0));

        Absence result = absenceService.updateAbsenceStatus(absence.getId(), AbsenceStatus.REJECTED);

        assertEquals(AbsenceStatus.REJECTED, result.getStatus());
        assertEquals(16, this.user.getNumberOfAbsences());
        verify(absenceRepository).save(absence);
    }

    @Test
    void testThatUpdateAbsenceStatusChangesStatusSuccessfullyWithoutUpdatingUsersAvailableDays() {
        when(absenceRepository.findById(absence.getId())).thenReturn(Optional.of(absence));
        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userxRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(absenceRepository.save(any(Absence.class))).thenAnswer(i -> i.getArgument(0));

        Absence result = absenceService.updateAbsenceStatus(absence.getId(), AbsenceStatus.APPROVED);

        assertEquals(AbsenceStatus.APPROVED, result.getStatus());
        assertNotEquals(25, this.user.getNumberOfAbsences());
        verify(absenceRepository).save(absence);
    }

    @Test
    void testThatUpdateAbsenceStatusThrowsNotFoundWhenAbsenceDoesNotExist() {
        when(absenceRepository.findById(absence.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> absenceService.updateAbsenceStatus(absence.getId(), AbsenceStatus.APPROVED));
    }

    @Test
    void testThatUpdateAbsenceStatusThrowsNotFoundWhenManagerDoesNotExist() {
        when(absenceRepository.findById(absence.getId())).thenReturn(Optional.of(absence));
        when(userxRepository.findById(manager.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> absenceService.updateAbsenceStatus(absence.getId(), AbsenceStatus.APPROVED));
    }

    @Test
    void testThatUpdateAbsenceStatusThrowsForbiddenWhenManagerLacksRights() {
        manager.setUserRoles(Collections.emptySet());
        when(absenceRepository.findById(absence.getId())).thenReturn(Optional.of(absence));
        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

        assertThrows(ForbiddenException.class, () -> absenceService.updateAbsenceStatus(absence.getId(), AbsenceStatus.APPROVED));
    }

    @Test
    void testThatUpdateAbsenceStatusThrowsForbiddenWhenManagerIsInDifferentDepartment() {
        Department otherDept = TestDataUtil.createDepartmentEntity(TestDataUtil.createBuildingEntity());
        otherDept.setId(UUID.randomUUID());
        manager.setMyRoom(TestDataUtil.createRoomEntity(otherDept));

        when(absenceRepository.findById(absence.getId())).thenReturn(Optional.of(absence));
        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userxRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class, () -> absenceService.updateAbsenceStatus(absence.getId(), AbsenceStatus.APPROVED));
    }

    @Test
    void testThatClockInSucceedsWhenUserHasNoRoom() {
        user.setMyRoom(null);
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(false).build();
        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));

        absenceService.clockIn(user);

        ArgumentCaptor<UserClockStatus> captor = ArgumentCaptor.forClass(UserClockStatus.class);
        verify(clockStatusRepository).save(captor.capture());
        assertTrue(captor.getValue().isClockedIn());
        verifyNoInteractions(roomRepository, roomOccupancyRepository, roomMonitoringRepository, eventPublisher);
    }

    @Test
    void testThatClockInCreatesNewStatusWhenNoneExists() {
        user.setMyRoom(null);
        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.empty());

        absenceService.clockIn(user);

        ArgumentCaptor<UserClockStatus> captor = ArgumentCaptor.forClass(UserClockStatus.class);
        verify(clockStatusRepository).save(captor.capture());
        assertTrue(captor.getValue().isClockedIn());
    }

    @Test
    void testThatClockInThrowsConflictWhenAlreadyClockedIn() {
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(true).build();
        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));

        assertThrows(ConflictException.class, () -> absenceService.clockIn(user));

        verify(clockStatusRepository, never()).save(any());
    }

    @Test
    void testThatClockInThrowsNotFoundWhenRoomDoesNotExist() {
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(false).build();
        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));
        when(roomRepository.existsById(room.getId())).thenReturn(false);

        assertThrows(NotFoundException.class, () -> absenceService.clockIn(user));

        verify(clockStatusRepository, never()).save(any());
    }

    @Test
    void testThatClockInIncrementsRoomOccupancyWhenRoomExists() {
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(false).build();
        RoomOccupancy occupancy = RoomOccupancy.builder()
                .roomId(room.getId()).peopleCnt(2).build();

        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));
        when(roomRepository.existsById(room.getId())).thenReturn(true);
        when(roomMonitoringRepository.findById(room.getId())).thenReturn(Optional.of(monitoringWithoutPi));
        when(roomOccupancyRepository.findById(room.getId().toString())).thenReturn(Optional.of(occupancy));

        absenceService.clockIn(user);

        ArgumentCaptor<RoomOccupancy> captor = ArgumentCaptor.forClass(RoomOccupancy.class);
        verify(roomOccupancyRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getPeopleCnt());
    }

    @Test
    void testThatClockInCreatesOccupancyRecordWhenNoneExists() {
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(false).build();

        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));
        when(roomRepository.existsById(room.getId())).thenReturn(true);
        when(roomMonitoringRepository.findById(room.getId())).thenReturn(Optional.of(monitoringWithoutPi));
        when(roomOccupancyRepository.findById(room.getId().toString())).thenReturn(Optional.empty());

        absenceService.clockIn(user);

        ArgumentCaptor<RoomOccupancy> captor = ArgumentCaptor.forClass(RoomOccupancy.class);
        verify(roomOccupancyRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getPeopleCnt());
    }

    @Test
    void testThatClockInPublishesEventWhenPiIsLinked() {
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(false).build();
        RoomOccupancy occupancy = RoomOccupancy.builder()
                .roomId(room.getId()).peopleCnt(0).build();

        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));
        when(roomRepository.existsById(room.getId())).thenReturn(true);
        when(roomMonitoringRepository.findById(room.getId())).thenReturn(Optional.of(monitoringWithPi));
        when(roomOccupancyRepository.findById(room.getId().toString())).thenReturn(Optional.of(occupancy));

        absenceService.clockIn(user);

        verify(eventPublisher).publishEvent(any(NotifyRaspberryCommand.class));
    }

    @Test
    void testThatClockInDoesNotPublishEventWhenNoPiLinked() {
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(false).build();
        RoomOccupancy occupancy = RoomOccupancy.builder()
                .roomId(room.getId()).peopleCnt(0).build();

        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));
        when(roomRepository.existsById(room.getId())).thenReturn(true);
        when(roomMonitoringRepository.findById(room.getId())).thenReturn(Optional.of(monitoringWithoutPi));
        when(roomOccupancyRepository.findById(room.getId().toString())).thenReturn(Optional.of(occupancy));

        absenceService.clockIn(user);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testThatClockOutSucceedsWhenUserHasNoRoom() {
        user.setMyRoom(null);
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(true).build();
        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));

        absenceService.clockOut(user);

        ArgumentCaptor<UserClockStatus> captor = ArgumentCaptor.forClass(UserClockStatus.class);
        verify(clockStatusRepository).save(captor.capture());
        assertFalse(captor.getValue().isClockedIn());
        verifyNoInteractions(roomRepository, roomOccupancyRepository, roomMonitoringRepository, eventPublisher);
    }

    @Test
    void testThatClockOutThrowsConflictWhenNeverClockedIn() {
        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.empty());

        assertThrows(ConflictException.class, () -> absenceService.clockOut(user));

        verify(clockStatusRepository, never()).save(any());
    }

    @Test
    void testThatClockOutThrowsConflictWhenAlreadyClockedOut() {
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(false).build();
        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));

        assertThrows(ConflictException.class, () -> absenceService.clockOut(user));

        verify(clockStatusRepository, never()).save(any());
    }

    @Test
    void testThatClockOutThrowsNotFoundWhenRoomDoesNotExist() {
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(true).build();
        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));
        when(roomRepository.existsById(room.getId())).thenReturn(false);

        assertThrows(NotFoundException.class, () -> absenceService.clockOut(user));

        verify(clockStatusRepository, never()).save(any());
    }

    @Test
    void testThatClockOutDecrementsRoomOccupancyWhenCountIsPositive() {
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(true).build();
        RoomOccupancy occupancy = RoomOccupancy.builder()
                .roomId(room.getId()).peopleCnt(3).build();

        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));
        when(roomRepository.existsById(room.getId())).thenReturn(true);
        when(roomMonitoringRepository.findById(room.getId())).thenReturn(Optional.of(monitoringWithoutPi));
        when(roomOccupancyRepository.findById(room.getId().toString())).thenReturn(Optional.of(occupancy));

        absenceService.clockOut(user);

        ArgumentCaptor<RoomOccupancy> captor = ArgumentCaptor.forClass(RoomOccupancy.class);
        verify(roomOccupancyRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getPeopleCnt());
    }

    @Test
    void testThatClockOutDoesNotDecrementBelowZero() {
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(true).build();
        RoomOccupancy occupancy = RoomOccupancy.builder()
                .roomId(room.getId()).peopleCnt(0).build();

        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));
        when(roomRepository.existsById(room.getId())).thenReturn(true);
        when(roomMonitoringRepository.findById(room.getId())).thenReturn(Optional.of(monitoringWithoutPi));
        when(roomOccupancyRepository.findById(room.getId().toString())).thenReturn(Optional.of(occupancy));

        absenceService.clockOut(user);

        ArgumentCaptor<RoomOccupancy> captor = ArgumentCaptor.forClass(RoomOccupancy.class);
        verify(roomOccupancyRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getPeopleCnt());
    }

    @Test
    void testThatClockOutCreatesOccupancyRecordWhenNoneExists() {
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(true).build();

        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));
        when(roomRepository.existsById(room.getId())).thenReturn(true);
        when(roomMonitoringRepository.findById(room.getId())).thenReturn(Optional.of(monitoringWithoutPi));
        when(roomOccupancyRepository.findById(room.getId().toString())).thenReturn(Optional.empty());

        absenceService.clockOut(user);

        ArgumentCaptor<RoomOccupancy> captor = ArgumentCaptor.forClass(RoomOccupancy.class);
        verify(roomOccupancyRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getPeopleCnt());
    }

    @Test
    void testThatClockOutPublishesEventWhenPiIsLinked() {
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(true).build();
        RoomOccupancy occupancy = RoomOccupancy.builder()
                .roomId(room.getId()).peopleCnt(1).build();

        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));
        when(roomRepository.existsById(room.getId())).thenReturn(true);
        when(roomMonitoringRepository.findById(room.getId())).thenReturn(Optional.of(monitoringWithPi));
        when(roomOccupancyRepository.findById(room.getId().toString())).thenReturn(Optional.of(occupancy));

        absenceService.clockOut(user);

        verify(eventPublisher).publishEvent(any(NotifyRaspberryCommand.class));
    }

    @Test
    void testThatClockOutDoesNotPublishEventWhenNoPiLinked() {
        UserClockStatus status = UserClockStatus.builder()
                .userId(user.getId()).clockedIn(true).build();
        RoomOccupancy occupancy = RoomOccupancy.builder()
                .roomId(room.getId()).peopleCnt(1).build();

        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(status));
        when(roomRepository.existsById(room.getId())).thenReturn(true);
        when(roomMonitoringRepository.findById(room.getId())).thenReturn(Optional.of(monitoringWithoutPi));
        when(roomOccupancyRepository.findById(room.getId().toString())).thenReturn(Optional.of(occupancy));

        absenceService.clockOut(user);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testThatClockStatusReturnsFalseIfNothingWasFound() {
        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.empty());

        boolean res = absenceService.isClockedIn(user);
        assertFalse(res);
    }

    @Test
    void testThatClockStatusReturnsTrueIfClockStatusIsTrue() {
        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(new UserClockStatus(user.getId(), true)));

        boolean res = absenceService.isClockedIn(user);
        assertTrue(res);
    }

    @Test
    void testThatClockStatusReturnsFalseIfClockStatusIsFalse() {
        when(clockStatusRepository.findById(user.getId().toString())).thenReturn(Optional.of(new UserClockStatus(user.getId(), false)));

        boolean res = absenceService.isClockedIn(user);
        assertFalse(res);
    }

    @Test
    void testThatCancelIllnessAbsenceSucceedsWhenValid() {
        UUID absenceId = absence.getId();
        absence.setStatus(AbsenceStatus.PENDING);
        int initialAbsences = user.getNumberOfAbsences();

        when(absenceRepository.findById(absenceId)).thenReturn(Optional.of(absence));
        when(userxRepository.save(any(Userx.class))).thenAnswer(i -> i.getArgument(0));
        when(absenceRepository.save(any(Absence.class))).thenAnswer(i -> i.getArgument(0));

        Absence result = absenceService.cancelAbsence(absenceId, user);

        assertNotNull(result);
        assertEquals(AbsenceStatus.CANCELLED, result.getStatus());
        assertEquals(initialAbsences, user.getNumberOfAbsences());
        verify(userxRepository).save(user);
        verify(absenceRepository).save(absence);
    }

    @Test
    void testThatCancelVacationAbsenceSucceedsWhenValid() {
        UUID absenceId = absence.getId();
        absence.setStatus(AbsenceStatus.PENDING);
        absence.setTypeOfAbsence(AbsenceType.VACATION);
        int initialAbsences = user.getNumberOfAbsences();
        long absenceDays = ChronoUnit.DAYS.between(absence.getStartDate().toLocalDate(), absence.getEndDate().toLocalDate());

        when(absenceRepository.findById(absenceId)).thenReturn(Optional.of(absence));
        when(userxRepository.save(any(Userx.class))).thenAnswer(i -> i.getArgument(0));
        when(absenceRepository.save(any(Absence.class))).thenAnswer(i -> i.getArgument(0));

        Absence result = absenceService.cancelAbsence(absenceId, user);

        assertNotNull(result);
        assertEquals(AbsenceStatus.CANCELLED, result.getStatus());
        Integer left = (int) (initialAbsences + absenceDays);
        assertEquals(left, user.getNumberOfAbsences());
        verify(userxRepository).save(user);
        verify(absenceRepository).save(absence);
    }

    @Test
    void testThatCancelAbsenceThrowsNotFoundWhenAbsenceDoesNotExist() {
        UUID missingId = UUID.randomUUID();
        when(absenceRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> absenceService.cancelAbsence(missingId, user));
        verify(userxRepository, never()).save(any());
        verify(absenceRepository, never()).save(any());
    }

    @Test
    void testThatCancelAbsenceThrowsForbiddenWhenUserDoesNotOwnAbsence() {
        UUID absenceId = absence.getId();
        Userx unauthorizedUser = new Userx();
        unauthorizedUser.setId(UUID.randomUUID()); // Different ID from the absence owner

        when(absenceRepository.findById(absenceId)).thenReturn(Optional.of(absence));

        assertThrows(ForbiddenException.class, () -> absenceService.cancelAbsence(absenceId, unauthorizedUser));
        verify(userxRepository, never()).save(any());
        verify(absenceRepository, never()).save(any());
    }

    @Test
    void testThatCancelAbsenceThrowsValidationExceptionWhenStatusIsNotPending() {
        UUID absenceId = absence.getId();
        absence.setStatus(AbsenceStatus.APPROVED);

        when(absenceRepository.findById(absenceId)).thenReturn(Optional.of(absence));

        assertThrows(ValidationException.class, () -> absenceService.cancelAbsence(absenceId, user));
        verify(userxRepository, never()).save(any());
        verify(absenceRepository, never()).save(any());
    }


    @Test
    void testThatGetAllAvailableManagersForUserReturnsOnlyManagersWithCorrectAuthority() {
        UUID deptId = dept.getId();

        Userx eligibleManager = mock(Userx.class);
        Userx ineligibleEmployee = mock(Userx.class);

        org.springframework.security.core.GrantedAuthority managerAuth = () -> "CAN_MANAGE_ABSENCES";
        org.springframework.security.core.GrantedAuthority employeeAuth = () -> "CAN_VIEW_MEASURES";

        when(eligibleManager.getAuthorities()).thenAnswer(i -> Set.of(managerAuth));
        when(ineligibleEmployee.getAuthorities()).thenAnswer(i -> Set.of(employeeAuth));
        when(userxRepository.findAllByDepartment(deptId)).thenReturn(List.of(eligibleManager, ineligibleEmployee));

        List<Userx> result = absenceService.getAllAvailableManagersForUser(user);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(eligibleManager));
        assertFalse(result.contains(ineligibleEmployee));
        verify(userxRepository).findAllByDepartment(deptId);
    }

    @Test
    void testThatGetAllAvailableManagersForUserReturnsEmptyListWhenNoManagersExist() {

        UUID deptId = dept.getId();
        when(userxRepository.findAllByDepartment(deptId)).thenReturn(Collections.emptyList());

        List<Userx> result = absenceService.getAllAvailableManagersForUser(user);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userxRepository).findAllByDepartment(deptId);
    }
}