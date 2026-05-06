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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceUnitTest {

    @Mock private RoomRepository roomRepository;
    @Mock private RoomMonitoringRepository monitoringRepository;
    @Mock private UserxRepository userxRepository;
    @Mock private RaspberryPiRepository raspberryPiRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private NotificationClient notificationClient;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room sampleRoom;
    private RoomMonitoring sampleMonitoring;
    private UUID roomId;
    private UUID deptId;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        deptId = UUID.randomUUID();

        Building building = TestDataUtil.createBuildingEntity();
        Department department = TestDataUtil.createDepartmentEntity(building);
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
        when(monitoringRepository.findById(roomId))
                .thenReturn(Optional.of(sampleMonitoring));

        roomService.deleteRoom(roomId);

        verify(roomRepository).deleteById(roomId);
        verify(monitoringRepository).deleteById(roomId);
    }

    @Test
    void testThatDeleteRoomUnlinksPiAndPublishesEventWhenPiLinked() {
        RaspberryPi pi = new RaspberryPi();
        pi.setId(UUID.randomUUID());

        sampleMonitoring.setRaspberryPi(pi);
        pi.setRoomMonitoring(sampleMonitoring);

        when(monitoringRepository.findById(roomId))
                .thenReturn(Optional.of(sampleMonitoring));

        roomService.deleteRoom(roomId);

        // Pi should be unlinked from monitoring
        assertNull(pi.getRoomMonitoring());

        verify(raspberryPiRepository).save(pi);
        verify(eventPublisher).publishEvent(any(NotifyRaspberryCommand.class));

        verify(roomRepository).deleteById(roomId);
        verify(monitoringRepository).deleteById(roomId);
    }

    @Test
    void testThatDeleteRoomSkipsMonitoringCleanupWhenMonitoringMissing() {
        when(monitoringRepository.findById(roomId))
                .thenReturn(Optional.empty());

        roomService.deleteRoom(roomId);

        // core deletions always happen
        verify(roomRepository).deleteById(roomId);
        verify(monitoringRepository).deleteById(roomId);

        // no side effects
        verifyNoInteractions(raspberryPiRepository, eventPublisher);
    }
}