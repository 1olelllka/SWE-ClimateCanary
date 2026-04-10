package at.qe.skeleton.tests.services;

import at.qe.skeleton.exceptions.ForbiddenException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.AbsenceRepository;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.services.impl.AbsenceServiceImpl;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AbsenceServiceUnitTests {

    @Mock private AbsenceRepository absenceRepository;
    @Mock private UserxRepository userxRepository;
    @InjectMocks private AbsenceServiceImpl absenceService;

    private Userx user;
    private Userx manager;
    private Absence absence;
    private Department dept;

    @BeforeEach
    void setUp() {
        dept = TestDataUtil.createDepartmentEntity(TestDataUtil.createBuildingEntity());
        dept.setId(UUID.randomUUID());

        Room room = TestDataUtil.createRoomEntity(dept);

        user = TestDataUtil.createUserxEntity(null, room);
        user.setId(UUID.randomUUID());

        manager = TestDataUtil.createUserxEntity(null, room);
        manager.setId(UUID.randomUUID());
        manager.setUserRoles(Set.of(UserRole.builder()
                .permissions(Set.of(Permission.CAN_MANAGE_ABSENCES)).build()));

        absence = TestDataUtil.createAbsence(user);
        absence.setId(UUID.randomUUID());
        absence.setAssignedTo(manager.getId());
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

    @Test
    void testThatCreateNewAbsenceSucceedsWhenValid() {
        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userxRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(absenceRepository.save(any(Absence.class))).thenReturn(absence);

        Absence result = absenceService.createNewAbsenceForUser(absence);

        assertNotNull(result);
        verify(absenceRepository).save(absence);
    }

    @Test
    void testThatCreateNewAbsenceThrowsValidationWhenAssigningToSelf() {
        absence.setAssignedTo(user.getId());

        assertThrows(ValidationException.class, () -> absenceService.createNewAbsenceForUser(absence));
    }

    @Test
    void testThatCreateNewAbsenceThrowsNotFoundWhenManagerDoesNotExist() {
        when(userxRepository.findById(manager.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> absenceService.createNewAbsenceForUser(absence));
    }

    @Test
    void testThatCreateNewAbsenceThrowsForbiddenWhenManagerLacksRights() {
        manager.setUserRoles(Collections.emptySet());
        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

        assertThrows(ForbiddenException.class, () -> absenceService.createNewAbsenceForUser(absence));
    }

    @Test
    void testThatCreateNewAbsenceThrowsForbiddenWhenManagerIsInDifferentDepartment() {
        Department otherDept = TestDataUtil.createDepartmentEntity(TestDataUtil.createBuildingEntity());
        otherDept.setId(UUID.randomUUID());
        manager.setMyRoom(TestDataUtil.createRoomEntity(otherDept));

        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userxRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class, () -> absenceService.createNewAbsenceForUser(absence));
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
    void testThatDeleteAbsenceByIdSucceedsForOwner() {
        when(absenceRepository.findById(absence.getId())).thenReturn(Optional.of(absence));

        absenceService.deleteAbsenceById(absence.getId(), user);

        verify(absenceRepository).deleteById(absence.getId());
    }

    @Test
    void testThatDeleteAbsenceByIdThrowsNotFoundWhenAbsenceDoesNotExist() {
        when(absenceRepository.findById(absence.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> absenceService.deleteAbsenceById(absence.getId(), user));
    }

    @Test
    void testThatDeleteAbsenceByIdThrowsForbiddenForNonOwner() {
        Userx otherUser = new Userx();
        otherUser.setId(UUID.randomUUID());
        when(absenceRepository.findById(absence.getId())).thenReturn(Optional.of(absence));

        assertThrows(ForbiddenException.class, () -> absenceService.deleteAbsenceById(absence.getId(), otherUser));
    }

    @Test
    void testThatUpdateAbsenceStatusChangesStatusSuccessfully() {
        when(absenceRepository.findById(absence.getId())).thenReturn(Optional.of(absence));
        when(userxRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userxRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(absenceRepository.save(any(Absence.class))).thenAnswer(i -> i.getArgument(0));

        Absence result = absenceService.updateAbsenceStatus(absence.getId(), AbsenceStatus.REJECTED);

        assertEquals(AbsenceStatus.REJECTED, result.getStatus());
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
}