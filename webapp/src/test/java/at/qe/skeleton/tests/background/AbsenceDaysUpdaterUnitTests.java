package at.qe.skeleton.tests.background;

import at.qe.skeleton.background.AbsenceDaysUpdater;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.UserxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbsenceDaysUpdaterUnitTests {

    @Mock
    private UserxRepository userxRepository;

    @InjectMocks
    private AbsenceDaysUpdater absenceDaysUpdater;

    private Userx employee1;
    private Userx employee2;
    private Userx deptManager;

    @BeforeEach
    void setUp() {
        employee1 = Userx.builder().id(UUID.randomUUID()).build();
        employee1.setNumberOfAbsences(10);

        employee2 = Userx.builder().id(UUID.randomUUID()).build();
        employee2.setNumberOfAbsences(0);

        deptManager = Userx.builder().id(UUID.randomUUID()).build();
        deptManager.setNumberOfAbsences(5);
    }

    @Test
    void updateAllAbsencesNumbersShouldAdd25ToAllEmployees() {
        when(userxRepository.findByRoleName("EMPLOYEE")).thenReturn(List.of(employee1, employee2));
        when(userxRepository.findByRoleName("DEPARTMENT_MANAGER")).thenReturn(Collections.emptyList());

        absenceDaysUpdater.updateAllAbsencesNumbers();

        assertEquals(35, employee1.getNumberOfAbsences());
        assertEquals(25, employee2.getNumberOfAbsences());
    }

    @Test
    void updateAllAbsencesNumbersShouldAdd25ToAllDepartmentManagers() {
        when(userxRepository.findByRoleName("EMPLOYEE")).thenReturn(Collections.emptyList());
        when(userxRepository.findByRoleName("DEPARTMENT_MANAGER")).thenReturn(List.of(deptManager));

        absenceDaysUpdater.updateAllAbsencesNumbers();

        assertEquals(30, deptManager.getNumberOfAbsences());
    }

    @Test
    void updateAllAbsencesNumbersShouldSaveAllEmployees() {
        when(userxRepository.findByRoleName("EMPLOYEE")).thenReturn(List.of(employee1, employee2));
        when(userxRepository.findByRoleName("DEPARTMENT_MANAGER")).thenReturn(Collections.emptyList());

        absenceDaysUpdater.updateAllAbsencesNumbers();

        verify(userxRepository, times(1)).save(employee1);
        verify(userxRepository, times(1)).save(employee2);
    }

    @Test
    void updateAllAbsencesNumbersShouldSaveAllDepartmentManagers() {
        when(userxRepository.findByRoleName("EMPLOYEE")).thenReturn(Collections.emptyList());
        when(userxRepository.findByRoleName("DEPARTMENT_MANAGER")).thenReturn(List.of(deptManager));

        absenceDaysUpdater.updateAllAbsencesNumbers();

        verify(userxRepository, times(1)).save(deptManager);
    }

    @Test
    void updateAllAbsencesNumbersShouldHandleNoEmployeesOrManagers() {
        when(userxRepository.findByRoleName("EMPLOYEE")).thenReturn(Collections.emptyList());
        when(userxRepository.findByRoleName("DEPARTMENT_MANAGER")).thenReturn(Collections.emptyList());

        absenceDaysUpdater.updateAllAbsencesNumbers();

        verify(userxRepository, never()).save(any());
    }

    @Test
    void updateAllAbsencesNumbersShouldUpdateBothRolesIndependently() {
        when(userxRepository.findByRoleName("EMPLOYEE")).thenReturn(List.of(employee1));
        when(userxRepository.findByRoleName("DEPARTMENT_MANAGER")).thenReturn(List.of(deptManager));

        absenceDaysUpdater.updateAllAbsencesNumbers();

        assertEquals(35, employee1.getNumberOfAbsences());
        assertEquals(30, deptManager.getNumberOfAbsences());
        verify(userxRepository, times(1)).save(employee1);
        verify(userxRepository, times(1)).save(deptManager);
    }
}