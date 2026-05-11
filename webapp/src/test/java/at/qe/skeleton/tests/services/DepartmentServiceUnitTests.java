package at.qe.skeleton.tests.services;

import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.ForbiddenException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Permission;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.impl.DepartmentServiceImpl;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceUnitTests {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department sampleDepartment;
    private UUID departmentId;

    @BeforeEach
    void setUp() {
        departmentId = UUID.randomUUID();
        Building building = TestDataUtil.createBuildingEntity();
        sampleDepartment = TestDataUtil.createDepartmentEntity(building);
        sampleDepartment.setId(departmentId);
        sampleDepartment.setName("Department of Informatics");
    }

    @Test
    void testThatGetPageOfDepartmentsShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Department> page = new PageImpl<>(List.of(sampleDepartment));
        when(departmentRepository.findAll(pageable)).thenReturn(page);

        Page<Department> result = departmentService.getPageOfDepartments(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(departmentRepository).findAll(pageable);
    }

    @Test
    void testThatGetDepartmentByIdWhenExistsShouldReturnDepartment() {
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(sampleDepartment));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(TestDataUtil.createUserxEntity(UserRole.builder().permissions(Set.of(Permission.CAN_VIEW_OWN_DEPARTMENT_MEASURES)).build(), null));
        Department result = departmentService.getDepartmentById(departmentId, false);

        assertEquals(sampleDepartment.getName(), result.getName());
        assertEquals(departmentId, result.getId());
    }

    @Test
    void testThatGetDepartmentByIdWhenExistsButRoleIsEmployeeShouldThrowException() {
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(sampleDepartment));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(TestDataUtil.createUserxEntity(UserRole.builder().permissions(Set.of()).build(), null));
        assertThrows(ForbiddenException.class, () -> departmentService.getDepartmentById(departmentId, false));
    }

    @Test
    void testThatGetDepartmentByIdWhenNotExistsShouldThrowNotFoundException() {
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> departmentService.getDepartmentById(departmentId, false));
    }

    @Test
    void testThatCreateDepartmentSuccessful() {
        when(departmentRepository.existsByNameAndBuildingId(any(), any())).thenReturn(false);
        when(departmentRepository.save(any())).thenReturn(sampleDepartment);

        Department result = departmentService.createDepartment(sampleDepartment);

        assertNotNull(result);
        verify(departmentRepository).save(sampleDepartment);
    }

    @Test
    void testThatCreateDepartmentShouldThrowConflictIfNameExists() {
        when(departmentRepository.existsByNameAndBuildingId(sampleDepartment.getName(), sampleDepartment.getBuilding().getId())).thenReturn(true);

        assertThrows(ConflictException.class, () -> departmentService.createDepartment(sampleDepartment));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void testThatPatchSpecificDepartmentUpdatesName() {
        Department patchData = new Department();
        patchData.setName("New Department Name");

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(sampleDepartment));
        when(departmentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Department result = departmentService.patchSpecificDepartment(departmentId, patchData);

        assertEquals("New Department Name", result.getName());
        verify(departmentRepository).save(any());
    }

    @Test
    void testThatPatchSpecificDepartmentShouldThrowNotFound() {
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                departmentService.patchSpecificDepartment(departmentId, new Department()));
    }

    @Test
    void testThatDeleteDepartmentShouldCallRepository() {
        departmentService.deleteDepartment(departmentId);
        verify(departmentRepository, times(1)).deleteById(departmentId);
    }
}