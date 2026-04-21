package at.qe.skeleton.tests.services;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.PiConfigDTO;
import at.qe.skeleton.dtos.UpdateType;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.NotifyDeadLetterRepository;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.RoomOccupancyRepository;
import at.qe.skeleton.services.impl.RaspberryServiceImpl;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RaspberryServiceUnitTests {

    @Mock
    private RaspberryPiRepository raspberryPiRepository;

    @Mock
    private RoomMonitoringRepository monitoringRepository;

    @Mock
    private NotifyDeadLetterRepository deadLetterRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private RoomOccupancyRepository occupancyRepository;

    @InjectMocks
    private RaspberryServiceImpl raspberryService;

    private RaspberryPi samplePi;
    private RoomMonitoring sampleRoom;
    private SensorStation sampleSensor;
    private UUID piId;
    private UUID roomId;
    private UUID sensorId;

    @BeforeEach
    void setUp() {
        piId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        sensorId = UUID.randomUUID();

        sampleSensor = new SensorStation();
        sampleSensor.setId(sensorId);

        sampleRoom = new RoomMonitoring();
        sampleRoom.setRoomId(roomId);
        sampleRoom.setSensorStation(sampleSensor);

        samplePi = new RaspberryPi();
        samplePi.setId(piId);
        samplePi.setName("Pi-01");
        samplePi.setIp("192.168.1.100");
        samplePi.setFrequency(5000);

        Set<RoomMonitoring> rooms = new HashSet<>();
        rooms.add(sampleRoom);
        samplePi.setRoomsMonitoring(rooms);
        sampleRoom.setRaspberryPi(samplePi);
    }

    @Test
    void testThatGetAllRaspberriesShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<RaspberryPi> page = new PageImpl<>(List.of(samplePi));
        when(raspberryPiRepository.findAll(pageable)).thenReturn(page);

        Page<RaspberryPi> result = raspberryService.getAllRaspberries(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(raspberryPiRepository).findAll(pageable);
    }

    @Test
    void testThatGetSpecificRaspberryReturnsPiWhenExists() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        RaspberryPi result = raspberryService.getSpecificRaspberry(piId);

        assertEquals(samplePi, result);
    }

    @Test
    void testThatGetSpecificRaspberryThrowsNotFoundWhenMissing() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.getSpecificRaspberry(piId));
    }

    @Test
    void testThatCreateNewRaspberrySucceeds() {
        when(raspberryPiRepository.existsByName("Pi-01")).thenReturn(false);
        when(raspberryPiRepository.save(any(RaspberryPi.class))).thenReturn(samplePi);

        RaspberryPi newPi = new RaspberryPi();
        newPi.setName("Pi-01");
        newPi.setIp("192.168.1.100");

        RaspberryPi result = raspberryService.createNewRaspberry(newPi);

        assertNotNull(result);
        verify(raspberryPiRepository).save(newPi);
    }

    @Test
    void testThatCreateNewRaspberryThrowsConflictOnDuplicateName() {
        when(raspberryPiRepository.existsByName("Pi-01")).thenReturn(true);

        assertThrows(ConflictException.class, () -> raspberryService.createNewRaspberry(samplePi));
        verify(raspberryPiRepository, never()).save(any());
    }

    @Test
    void testThatUpdateRaspberryByIdUpdatesOnlyProvidedFields() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(raspberryPiRepository.save(any(RaspberryPi.class))).thenAnswer(i -> i.getArgument(0));

        RaspberryPi patchData = new RaspberryPi();
        patchData.setFrequency(9999);

        RaspberryPi result = raspberryService.updateRaspberryById(piId, patchData);

        assertEquals(9999, result.getFrequency());
        assertEquals("Pi-01", result.getName());
        assertEquals("192.168.1.100", result.getIp());
        verify(raspberryPiRepository, never()).existsByName(anyString());
    }

    @Test
    void testThatUpdateRaspberryByIdSkipsConflictCheckWhenNameIsUnchanged() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(raspberryPiRepository.save(any(RaspberryPi.class))).thenAnswer(i -> i.getArgument(0));

        RaspberryPi patchData = new RaspberryPi();
        patchData.setName("Pi-01");

        RaspberryPi result = raspberryService.updateRaspberryById(piId, patchData);

        assertEquals("Pi-01", result.getName());
        verify(raspberryPiRepository, never()).existsByName(anyString());
    }

    @Test
    void testThatUpdateRaspberryByIdThrowsConflictOnDuplicateName() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(raspberryPiRepository.existsByName("Pi-02")).thenReturn(true);

        RaspberryPi patchData = new RaspberryPi();
        patchData.setName("Pi-02");

        assertThrows(ConflictException.class, () -> raspberryService.updateRaspberryById(piId, patchData));
        verify(raspberryPiRepository, never()).save(any());
    }

    @Test
    void testThatDeleteRaspberrySucceeds() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        raspberryService.deleteRaspberry(piId);

        assertNull(sampleRoom.getRaspberryPi());
        verify(monitoringRepository).saveAll(anyCollection());
        verify(raspberryPiRepository).deleteById(piId);
    }

    @Test
    void testThatDeleteRaspberryDoesNotDeleteIfRaspberryWasNotFound() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.empty());

        raspberryService.deleteRaspberry(piId);

        verify(raspberryPiRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void testThatGetOccupancyFromRedisReturnsMockedValue() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(occupancyRepository.findAllById(samplePi.getRoomsMonitoring().stream().map(r -> r.getRoomId().toString()).toList())).thenReturn(List.of(RoomOccupancy.builder().roomId(sampleRoom.getRoomId()).build()));

        // When Redis logic is added, one will mock the RedisTemplate here
        List<RoomOccupancy> occupancy = raspberryService.getOccupancyFromRedis(piId);

        assertEquals(1, occupancy.size());
    }

    @Test
    void testThatGetConfigForRaspberryNavigatesGraphCorrectly() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        PiConfigDTO config = raspberryService.getConfigForRaspberry(piId);

        assertNotNull(config);
        assertEquals(5000, config.frequency());
        assertTrue(config.sensors().contains(sensorId));
    }

    @Test
    void testThatGetConfigForRaspberryReturnEmptySensorsWhenNotLinked() {
        sampleRoom.setSensorStation(null);
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        PiConfigDTO config = raspberryService.getConfigForRaspberry(piId);

        assertNotNull(config);
        assertEquals(0, config.sensors().size());
    }

    @Test
    void testThatGetConfigForRaspberryThrowsNotFoundWhenPiMissing() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.getConfigForRaspberry(piId));
    }

    @Test
    void testThatRetryConnectionSucceeds() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        raspberryService.retryConnection(piId);

        verify(eventPublisher).publishEvent(any(NotifyRaspberryCommand.class));
        verify(deadLetterRepository, times(1)).findByRaspberryPi(piId);
        verify(deadLetterRepository, times(1)).deleteAll(anyList());
    }

    @Test
    void testThatRetryConnectionWorksWhenRoomsMonitoringIsNull() {
        samplePi.setRoomsMonitoring(null);
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        raspberryService.retryConnection(piId);

        // Uses the alternative `else` block to notify setup without iterating over rooms
        verify(eventPublisher, times(1)).publishEvent(any(NotifyRaspberryCommand.class));
        verify(deadLetterRepository, never()).findByRaspberryPi(any(UUID.class));
    }

    @Test
    void testThatRetryConnectionReplaysNonSetupDeadLetters() {
        NotifyDeadLetter setupLetter = new NotifyDeadLetter();
        setupLetter.setUpdateType(UpdateType.SETUP);
        setupLetter.setTriggeredAt(LocalDateTime.now());

        NotifyDeadLetter otherLetter = new NotifyDeadLetter();
        otherLetter.setUpdateType(UpdateType.SENSORS);
        otherLetter.setTriggeredAt(LocalDateTime.now());

        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(deadLetterRepository.findByRaspberryPi(piId)).thenReturn(List.of(setupLetter, otherLetter));

        raspberryService.retryConnection(piId);

        // Initial SETUP + one replayed non-SETUP letter = 2 events
        verify(eventPublisher, times(2)).publishEvent(any(NotifyRaspberryCommand.class));
        verify(deadLetterRepository).deleteAll(anyList());
    }

    @Test
    void testThatRetryConnectionSkipsSetupDeadLetters() {
        NotifyDeadLetter setupLetter = new NotifyDeadLetter();
        setupLetter.setUpdateType(UpdateType.SETUP);
        setupLetter.setTriggeredAt(LocalDateTime.now());

        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(deadLetterRepository.findByRaspberryPi(piId)).thenReturn(List.of(setupLetter));

        raspberryService.retryConnection(piId);

        // Only the initial SETUP event is sent, the dead letter one is filtered out
        verify(eventPublisher, times(1)).publishEvent(any(NotifyRaspberryCommand.class));
    }

    @Test
    void testThatRetryConnectionThrowsNotFoundWhenPiMissing() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.retryConnection(piId));
        verify(eventPublisher, never()).publishEvent(any());
        verify(deadLetterRepository, never()).findByRaspberryPi(any());
    }

    @Test
    void testThatAddNewRoomSucceeds() {
        RoomMonitoring unassignedRoom = new RoomMonitoring();
        unassignedRoom.setRoomId(UUID.randomUUID());

        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(monitoringRepository.findById(unassignedRoom.getRoomId())).thenReturn(Optional.of(unassignedRoom));
        when(monitoringRepository.save(any(RoomMonitoring.class))).thenAnswer(i -> i.getArgument(0));
        when(raspberryPiRepository.save(any(RaspberryPi.class))).thenAnswer(i -> i.getArgument(0));

        RaspberryPi result = raspberryService.addNewRoom(piId, unassignedRoom.getRoomId());

        assertNotNull(result);
        assertEquals(samplePi, unassignedRoom.getRaspberryPi());
        verify(monitoringRepository).save(unassignedRoom);
        verify(raspberryPiRepository).save(samplePi);
    }

    @Test
    void testThatAddNewRoomThrowsNotFoundWhenPiMissing() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.addNewRoom(piId, roomId));
    }

    @Test
    void testThatAddNewRoomThrowsNotFoundWhenRoomMissing() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.addNewRoom(piId, roomId));
    }

    @Test
    void testThatAddNewRoomThrowsConflictWhenRoomAlreadyAssigned() {
        RoomMonitoring assignedRoom = new RoomMonitoring();
        assignedRoom.setRoomId(UUID.randomUUID());
        assignedRoom.setRaspberryPi(new RaspberryPi()); // Already assigned to another Pi

        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(monitoringRepository.findById(assignedRoom.getRoomId())).thenReturn(Optional.of(assignedRoom));

        assertThrows(ConflictException.class, () -> raspberryService.addNewRoom(piId, assignedRoom.getRoomId()));
        verify(monitoringRepository, never()).save(any());
        verify(raspberryPiRepository, never()).save(any());
    }

    @Test
    void testThatRemoveRoomFromRaspberrySucceeds() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(monitoringRepository.save(any(RoomMonitoring.class))).thenAnswer(i -> i.getArgument(0));
        when(raspberryPiRepository.save(any(RaspberryPi.class))).thenAnswer(i -> i.getArgument(0));

        RaspberryPi result = raspberryService.removeRoomFromRaspberry(piId, roomId);

        assertNotNull(result);
        assertNull(sampleRoom.getRaspberryPi());
        verify(monitoringRepository).save(sampleRoom);
        verify(raspberryPiRepository).save(samplePi);
    }

    @Test
    void testThatRemoveRoomFromRaspberryThrowsNotFoundWhenPiMissing() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.removeRoomFromRaspberry(piId, roomId));
    }

    @Test
    void testThatRemoveRoomFromRaspberryThrowsNotFoundWhenRoomMissing() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.removeRoomFromRaspberry(piId, roomId));
    }
}