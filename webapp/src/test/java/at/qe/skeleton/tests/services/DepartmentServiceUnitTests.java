package at.qe.skeleton.tests.services;

import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.impl.DepartmentServiceImpl;
import at.qe.skeleton.services.impl.RoomServiceImpl;
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

    @Mock private DepartmentRepository departmentRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private RoomMonitoringRepository monitoringRepository;
    @Mock private RaspberryPiRepository raspberryPiRepository;
    @Mock private BuildingTrendRepository trendRepository;
    @Mock private RoomServiceImpl roomService;
    @Mock private AuthenticatedUserService authenticatedUserService;
    @Mock private AggregatedDepartmentStatsRepository departmentStatsRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department sampleDepartment;
    private Building sampleBuilding;
    private UUID departmentId;

    @BeforeEach
    void setUp() {
        departmentId = UUID.randomUUID();
        sampleBuilding = TestDataUtil.createBuildingEntity();
        sampleDepartment = TestDataUtil.createDepartmentEntity(sampleBuilding);
        sampleDepartment.setId(departmentId);
        sampleDepartment.setName("Department of Informatics");
    }

    // --- getPageOfDepartments ---

    @Test
    void testThatGetPageOfDepartmentsReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Department> page = new PageImpl<>(List.of(sampleDepartment));
        when(departmentRepository.findAll(pageable)).thenReturn(page);

        Page<Department> result = departmentService.getPageOfDepartments(pageable);

        assertEquals(1, result.getTotalElements());
        verify(departmentRepository).findAll(pageable);
    }

    // --- getDepartmentById ---

    @Test
    void testThatGetDepartmentByIdReturnsAllRoomsForPrivilegedUser() {
        UserRole role = UserRole.builder()
                .permissions(Set.of(Permission.CAN_VIEW_OWN_DEPARTMENT_MEASURES))
                .build();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(sampleDepartment));
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(TestDataUtil.createUserxEntity(role, null));

        Department result = departmentService.getDepartmentById(departmentId);

        assertEquals(departmentId, result.getId());
        assertEquals(sampleDepartment.getRooms(), result.getRooms()); // all rooms returned
    }

    @Test
    void testThatGetDepartmentByIdReturnsOnlySharedRoomsForUnprivilegedUser() {
        Room sharedRoom = TestDataUtil.createRoomEntity(sampleDepartment);
        sharedRoom.setRoomType(RoomType.SHARED);
        Room officeRoom = TestDataUtil.createRoomEntity(sampleDepartment);
        officeRoom.setRoomType(RoomType.OFFICE);
        sampleDepartment.setRooms(List.of(sharedRoom, officeRoom));

        UserRole role = UserRole.builder().permissions(Set.of()).build();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(sampleDepartment));
        when(authenticatedUserService.getAuthenticatedUser())
                .thenReturn(TestDataUtil.createUserxEntity(role, null));

        Department result = departmentService.getDepartmentById(departmentId);

        assertEquals(1, result.getRooms().size());
        assertEquals(RoomType.SHARED, result.getRooms().get(0).getRoomType());
    }

    @Test
    void testThatGetDepartmentByIdThrowsNotFoundWhenMissing() {
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> departmentService.getDepartmentById(departmentId));
    }

    // --- createDepartment ---

    @Test
    void testThatCreateDepartmentSucceeds() {
        when(departmentRepository.existsByNameAndBuildingId(any(), any())).thenReturn(false);
        when(departmentRepository.save(any())).thenReturn(sampleDepartment);

        Department result = departmentService.createDepartment(sampleDepartment);

        assertNotNull(result);
        verify(departmentRepository).save(sampleDepartment);
    }

    @Test
    void testThatCreateDepartmentThrowsConflictWhenNameExists() {
        when(departmentRepository.existsByNameAndBuildingId(
                sampleDepartment.getName(), sampleBuilding.getId())).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> departmentService.createDepartment(sampleDepartment));
        verify(departmentRepository, never()).save(any());
    }

    // --- createDepartmentWithRooms ---

    @Test
    void testThatCreateDepartmentWithRoomsAssignsExistingRooms() {
        UUID existingRoomId = UUID.randomUUID();
        Room existingRoom = TestDataUtil.createRoomEntity(sampleDepartment);
        existingRoom.setId(existingRoomId);

        when(departmentRepository.existsByNameAndBuildingId(any(), any())).thenReturn(false);
        when(departmentRepository.save(any())).thenReturn(sampleDepartment);
        when(roomRepository.findById(existingRoomId)).thenReturn(Optional.of(existingRoom));
        when(roomRepository.existsByRoomNumberAndDepartmentId(any(), any())).thenReturn(false);
        when(roomRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        departmentService.createDepartmentWithRooms(sampleDepartment, List.of(existingRoomId), List.of());

        assertEquals(sampleDepartment, existingRoom.getDepartment());
        verify(roomRepository).save(existingRoom);
    }

    @Test
    void testThatCreateDepartmentWithRoomsThrowsConflictWhenNameExists() {
        when(departmentRepository.existsByNameAndBuildingId(any(), any())).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                departmentService.createDepartmentWithRooms(sampleDepartment, List.of(), List.of()));
    }

    @Test
    void testThatCreateDepartmentWithRoomsThrowsNotFoundWhenExistingRoomMissing() {
        UUID missingId = UUID.randomUUID();
        when(departmentRepository.existsByNameAndBuildingId(any(), any())).thenReturn(false);
        when(departmentRepository.save(any())).thenReturn(sampleDepartment);
        when(roomRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                departmentService.createDepartmentWithRooms(sampleDepartment, List.of(missingId), List.of()));
    }

    // --- editDepartmentWithRooms ---

    @Test
    void testThatEditDepartmentWithRoomsSucceedsWithAllOperations() {
        // Arrange
        Department updatedDept = new Department();
        updatedDept.setName("Updated Informatics");
        updatedDept.setBuilding(sampleBuilding);

        UUID roomIdToDelete = UUID.randomUUID();
        UUID existingRoomIdToAssign = UUID.randomUUID();

        Room monitoringRoom = new Room();
        monitoringRoom.setId(roomIdToDelete);

        RaspberryPi mockPi = mock(RaspberryPi.class);
        RoomMonitoring mockMonitoring = mock(RoomMonitoring.class);
        when(mockMonitoring.getRaspberryPi()).thenReturn(mockPi);

        Room existingRoom = new Room();
        existingRoom.setId(existingRoomIdToAssign);
        existingRoom.setRoomNumber("101");

        Room newRoom = new Room();
        newRoom.setRoomNumber("202");
        Room savedNewRoom = new Room();
        savedNewRoom.setId(UUID.randomUUID());
        savedNewRoom.setRoomNumber("202");

        // Mocking Department operations
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(sampleDepartment));
        when(departmentRepository.existsByNameAndBuildingId("Updated Informatics", sampleBuilding.getId())).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(i -> i.getArgument(0));

        // Mocking Room deletions
        when(monitoringRepository.findById(roomIdToDelete)).thenReturn(Optional.of(mockMonitoring));

        // Mocking Room assignments
        when(roomRepository.findById(existingRoomIdToAssign)).thenReturn(Optional.of(existingRoom));
        when(roomRepository.existsByRoomNumberAndDepartmentId("101", departmentId)).thenReturn(false);
        when(roomRepository.save(existingRoom)).thenReturn(existingRoom);

        // Mocking New Room creations
        when(roomRepository.existsByRoomNumberAndDepartmentId("202", departmentId)).thenReturn(false);
        when(roomRepository.save(newRoom)).thenReturn(savedNewRoom);

        // Act
        Department result = departmentService.editDepartmentWithRooms(
                departmentId, updatedDept, List.of(roomIdToDelete), List.of(existingRoomIdToAssign), List.of(newRoom)
        );

        // Assert
        assertNotNull(result);
        assertEquals("Updated Informatics", result.getName());

        // Verify deletion steps
        verify(mockPi).setRoomMonitoring(null);
        verify(raspberryPiRepository).save(mockPi);
        verify(monitoringRepository).deleteById(roomIdToDelete);
        verify(roomRepository).deleteById(roomIdToDelete);

        // Verify assignment steps
        assertEquals(sampleDepartment, existingRoom.getDepartment());
        verify(roomRepository).save(existingRoom);

        // Verify new room steps
        assertEquals(sampleDepartment, newRoom.getDepartment());
        verify(roomRepository).save(newRoom);
        verify(monitoringRepository).save(any(RoomMonitoring.class));
    }

    @Test
    void testThatEditDepartmentWithRoomsThrowsNotFoundWhenDepartmentMissing() {
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                departmentService.editDepartmentWithRooms(departmentId, sampleDepartment, List.of(), List.of(), List.of())
        );
    }

    @Test
    void testThatEditDepartmentWithRoomsThrowsConflictWhenNewNameExists() {
        Department updatedDept = new Department();
        updatedDept.setName("Conflicting Name");

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(sampleDepartment));
        when(departmentRepository.existsByNameAndBuildingId("Conflicting Name", sampleBuilding.getId())).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                departmentService.editDepartmentWithRooms(departmentId, updatedDept, List.of(), List.of(), List.of())
        );
    }

    @Test
    void testThatEditDepartmentWithRoomsThrowsNotFoundWhenAssignedRoomMissing() {
        UUID missingRoomId = UUID.randomUUID();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(sampleDepartment));
        when(roomRepository.findById(missingRoomId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                departmentService.editDepartmentWithRooms(departmentId, sampleDepartment, List.of(), List.of(missingRoomId), List.of())
        );
    }

    @Test
    void testThatEditDepartmentWithRoomsThrowsConflictWhenAssignedRoomNumberAlreadyExistsInDept() {
        UUID assignedRoomId = UUID.randomUUID();
        Room existingRoom = new Room();
        existingRoom.setId(assignedRoomId);
        existingRoom.setRoomNumber("303");

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(sampleDepartment));
        when(roomRepository.findById(assignedRoomId)).thenReturn(Optional.of(existingRoom));
        when(roomRepository.existsByRoomNumberAndDepartmentId("303", departmentId)).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                departmentService.editDepartmentWithRooms(departmentId, sampleDepartment, List.of(), List.of(assignedRoomId), List.of())
        );
    }

    @Test
    void testThatEditDepartmentWithRoomsThrowsConflictWhenNewRoomNumberAlreadyExistsInDept() {
        Room newRoom = new Room();
        newRoom.setRoomNumber("404");

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(sampleDepartment));
        when(roomRepository.existsByRoomNumberAndDepartmentId("404", departmentId)).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                departmentService.editDepartmentWithRooms(departmentId, sampleDepartment, List.of(), List.of(), List.of(newRoom))
        );
    }

    // --- deleteDepartment ---

    @Test
    void testThatDeleteDepartmentDeletesDepartmentAndTrends() {
        sampleDepartment.setRooms(List.of()); // no rooms
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(sampleDepartment));

        departmentService.deleteDepartment(departmentId);

        verify(departmentRepository).delete(sampleDepartment);
        verify(trendRepository).deleteAllByDepartmentId(departmentId);
        verify(departmentStatsRepository).deleteAllByDepartmentId(departmentId);
    }

    @Test
    void testThatDeleteDepartmentDeletesRoomsBeforeDepartment() {
        UUID roomId1 = UUID.randomUUID();
        UUID roomId2 = UUID.randomUUID();
        Room room1 = TestDataUtil.createRoomEntity(sampleDepartment);
        room1.setId(roomId1);
        Room room2 = TestDataUtil.createRoomEntity(sampleDepartment);
        room2.setId(roomId2);
        sampleDepartment.setRooms(List.of(room1, room2));

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(sampleDepartment));

        departmentService.deleteDepartment(departmentId);

        verify(roomService).deleteRoom(roomId1);
        verify(roomService).deleteRoom(roomId2);
        verify(departmentRepository).delete(sampleDepartment);
        verify(trendRepository).deleteAllByDepartmentId(departmentId);
        verify(departmentStatsRepository).deleteAllByDepartmentId(departmentId);
    }

    @Test
    void testThatDeleteDepartmentStillDeletesTrendsWhenDepartmentNotFound() {
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        departmentService.deleteDepartment(departmentId);

        verify(departmentRepository, never()).delete(any());
        verify(trendRepository).deleteAllByDepartmentId(departmentId);
        verify(departmentStatsRepository).deleteAllByDepartmentId(departmentId);
    }
}