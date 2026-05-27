package at.qe.skeleton.tests.services;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.LimitDTO;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.impl.RoomServiceImpl;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceUnitTest {

    @Mock private RoomRepository roomRepository;
    @Mock private RoomMonitoringRepository monitoringRepository;
    @Mock private UserxRepository userxRepository;
    @Mock private RaspberryPiRepository raspberryPiRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private NotificationClient notificationClient;
    @Mock private AggregatedStatsRepository aggregatedStatsRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room sampleRoom;
    private RoomMonitoring sampleMonitoring;
    private UUID roomId;
    private UUID deptId;
    private Department department;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        deptId = UUID.randomUUID();

        Building building = TestDataUtil.createBuildingEntity();
        department = TestDataUtil.createDepartmentEntity(building);
        department.setId(deptId);

        sampleRoom = TestDataUtil.createRoomEntity(department);
        sampleRoom.setId(roomId);

        sampleMonitoring = RoomMonitoring.builder()
                .roomId(roomId)
                .roomNumber(sampleRoom.getRoomNumber())
                .humLimit(HumidityLimit.builder().minVal(30f).maxVal(70f).build())
                .tempLimit(TemperatureLimit.builder().minVal(18f).maxVal(26f).build())
                .polLimit(PollutionLimit.builder().maxVal(1000f).build())
                .build();
    }

    // --- getPageOfRooms ---

    @Test
    void testThatGetPageOfRoomsDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> page = new PageImpl<>(List.of(sampleRoom));

        when(roomRepository.findAll(pageable)).thenReturn(page);

        Page<Room> result = roomService.getPageOfRooms(pageable);

        assertEquals(1, result.getTotalElements());
        verify(roomRepository).findAll(pageable);
    }

    // --- createRoom ---

    @Test
    void testThatCreateRoomSucceeds() {
        when(departmentRepository.existsById(deptId)).thenReturn(true);
        when(roomRepository.existsByRoomNumberAndDepartmentId(sampleRoom.getRoomNumber(), deptId))
                .thenReturn(false);
        when(roomRepository.save(sampleRoom)).thenReturn(sampleRoom);

        Room result = roomService.createRoom(sampleRoom);

        assertNotNull(result);
        verify(monitoringRepository).save(any(RoomMonitoring.class));
    }

    @Test
    void testThatCreateRoomThrowsConflictWhenRoomNumberExists() {

        when(departmentRepository.existsById(deptId)).thenReturn(true);

        when(roomRepository.existsByRoomNumberAndDepartmentId(
                sampleRoom.getRoomNumber(),
                deptId
        )).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> roomService.createRoom(sampleRoom));

        verify(roomRepository, never()).save(any());
        verify(monitoringRepository, never()).save(any());
    }

    @Test
    void testThatCreateRoomThrowsNotFoundWhenDepartmentMissing() {
        when(departmentRepository.existsById(deptId)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> roomService.createRoom(sampleRoom));
    }

    // --- patchRoom ---

    @Test
    void testThatPatchRoomUpdatesScalarFields() {
        Room patch = Room.builder()
                .roomType(RoomType.OFFICE)
                .defaultPeopleCnt(5)
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Room result = roomService.patchRoom(roomId, patch);

        assertEquals(RoomType.OFFICE, result.getRoomType());
        assertEquals(5, result.getDefaultPeopleCnt());
        verify(roomRepository).save(any());
    }

    @Test
    void testThatPatchRoomUpdatesRoomNumberAndSyncsMonitoring() {
        Room patch = Room.builder()
                .roomNumber("NEW-101")
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.existsByRoomNumberAndDepartmentId("NEW-101", deptId))
                .thenReturn(false);

        when(monitoringRepository.findById(roomId))
                .thenReturn(Optional.of(sampleMonitoring));

        when(roomRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Room result = roomService.patchRoom(roomId, patch);

        assertEquals("NEW-101", result.getRoomNumber());
        assertEquals("NEW-101", sampleMonitoring.getRoomNumber());

        verify(monitoringRepository).save(sampleMonitoring);
    }

    @Test
    void testThatPatchRoomThrowsConflictWhenRoomNumberAlreadyExists() {
        Room patch = Room.builder()
                .roomNumber("TAKEN-102")
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.existsByRoomNumberAndDepartmentId("TAKEN-102", deptId))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> roomService.patchRoom(roomId, patch));

        verify(roomRepository, never()).save(any());
    }

    @Test
    void testThatPatchRoomThrowsNotFoundWhenRoomMissing() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> roomService.patchRoom(roomId, Room.builder().build()));
    }

    // --- getRoomMonitoring ---

    @Test
    void testThatGetRoomMonitoringReturnsMonitoringWhenExists() {
        when(monitoringRepository.findById(roomId))
                .thenReturn(Optional.of(sampleMonitoring));

        RoomMonitoring result = roomService.getRoomMonitoring(roomId);

        assertEquals(sampleMonitoring, result);
    }

    @Test
    void testThatGetRoomMonitoringThrowsNotFoundWhenMissing() {
        when(monitoringRepository.findById(roomId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> roomService.getRoomMonitoring(roomId));
    }

    // --- updateLimits ---

    @Test
    void testThatUpdateLimitsAppliesValues() {
        LimitDTO dto = new LimitDTO(roomId, 20f, 25f, 40f, 60f, 800f);

        when(monitoringRepository.findById(roomId))
                .thenReturn(Optional.of(sampleMonitoring));
        when(monitoringRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        RoomMonitoring result = roomService.updateLimits(roomId, dto);

        assertEquals(20f, result.getTempLimit().getMinVal());
        assertEquals(25f, result.getTempLimit().getMaxVal());
    }

    @Test
    void testThatUpdateLimitsThrowsNotFoundWhenRoomMissing() {
        when(monitoringRepository.findById(roomId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> roomService.updateLimits(roomId,
                        new LimitDTO(null, null, null, null, null, null)));
    }

// ---------------- deleteRoom ----------------

    @Test
    void testThatDeleteRoomDeletesBothRoomAndMonitoring() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.of(sampleMonitoring));

        roomService.deleteRoom(roomId);

        verify(roomRepository).delete(sampleRoom);
        verify(monitoringRepository).deleteById(roomId);
    }

    @Test
    void testThatDeleteRoomUnlinksUsersBeforeDeleting() {
        Userx user1 = new Userx();
        user1.setId(UUID.randomUUID());
        user1.setMyRoom(sampleRoom);

        Userx user2 = new Userx();
        user2.setId(UUID.randomUUID());
        user2.setMyRoom(sampleRoom);

        sampleRoom.setUsers(new HashSet<>(Set.of(user1, user2)));

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.of(sampleMonitoring));

        roomService.deleteRoom(roomId);

        assertNull(user1.getMyRoom());
        assertNull(user2.getMyRoom());
        verify(userxRepository, times(2)).save(any(Userx.class));
        verify(aggregatedStatsRepository).deleteAllByRoomId(roomId);
    }

    @Test
    void testThatDeleteRoomRemovesRoomFromDepartmentCollection() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.of(sampleMonitoring));

        roomService.deleteRoom(roomId);

        assertFalse(department.getRooms().contains(sampleRoom));
        assertNull(sampleRoom.getDepartment());
        verify(aggregatedStatsRepository).deleteAllByRoomId(roomId);
    }

    @Test
    void testThatDeleteRoomUnlinksPiAndPublishesEventWhenPiLinked() {
        RaspberryPi pi = new RaspberryPi();
        pi.setId(UUID.randomUUID());
        pi.setIp("localhost");
        pi.setPort(8080);

        sampleMonitoring.setRaspberryPi(pi);
        pi.setRoomMonitoring(sampleMonitoring);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.of(sampleMonitoring));

        roomService.deleteRoom(roomId);

        assertNull(pi.getRoomMonitoring());
        verify(raspberryPiRepository).save(pi);
        verify(eventPublisher).publishEvent(any(NotifyRaspberryCommand.class));
        verify(roomRepository).delete(sampleRoom);
        verify(monitoringRepository).deleteById(roomId);
        verify(aggregatedStatsRepository).deleteAllByRoomId(roomId);
    }

    @Test
    void testThatDeleteRoomSkipsRoomCleanupWhenRoomNotFound() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.of(sampleMonitoring));

        roomService.deleteRoom(roomId);

        verify(roomRepository, never()).delete(any());
        verifyNoInteractions(userxRepository);
        verify(monitoringRepository).deleteById(roomId);
        verify(aggregatedStatsRepository).deleteAllByRoomId(roomId);
    }

    @Test
    void testThatDeleteRoomSkipsMonitoringCleanupWhenMonitoringMissing() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.empty());

        roomService.deleteRoom(roomId);

        verify(roomRepository).delete(sampleRoom);
        verify(monitoringRepository).deleteById(roomId);
        verifyNoInteractions(raspberryPiRepository, eventPublisher);
        verify(aggregatedStatsRepository).deleteAllByRoomId(roomId);
    }
}