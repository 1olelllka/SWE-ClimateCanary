package at.qe.skeleton.tests.services;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.UpdateType;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.SensorStationRepository;
import at.qe.skeleton.services.impl.SensorStationServiceImpl;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SensorStationServiceUnitTests {

    @Mock
    private SensorStationRepository sensorRepository;

    @Mock
    private RoomMonitoringRepository monitoringRepository;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SensorStationServiceImpl sensorService;

    private SensorStation sampleStation;
    private RoomMonitoring sampleRoom;
    private UUID stationId;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        stationId = UUID.randomUUID();
        roomId = UUID.randomUUID();

        sampleRoom = new RoomMonitoring();
        sampleRoom.setRoomId(roomId);

        sampleStation = new SensorStation();
        sampleStation.setId(stationId);
        sampleStation.setName("Station-01");
        sampleStation.setRoomMonitoring(sampleRoom);

        // Circular link as expected by service logic
        sampleRoom.setSensorStation(sampleStation);
    }

    // --- getAllSensorStations ---

    @Test
    void testThatGetAllSensorStationsReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SensorStation> page = new PageImpl<>(List.of(sampleStation));
        when(sensorRepository.findAll(pageable)).thenReturn(page);

        Page<SensorStation> result = sensorService.getAllSensorStations(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(sensorRepository).findAll(pageable);
    }

    // --- createNewSensorStation ---

    @Test
    void testThatCreateNewSensorStationSucceeds() {
        when(sensorRepository.existsByName("Station-01")).thenReturn(false);
        when(sensorRepository.save(any(SensorStation.class))).thenReturn(sampleStation);
        when(monitoringRepository.save(any(RoomMonitoring.class))).thenReturn(sampleRoom);

        SensorStation result = sensorService.createNewSensorStation(sampleStation);

        assertNotNull(result);
        verify(sensorRepository).save(sampleStation);
        verify(monitoringRepository).save(sampleRoom);
        verify(eventPublisher).publishEvent(any(NotifyRaspberryCommand.class));
    }

    @Test
    void testThatCreateNewSensorStationThrowsConflictIfNameExists() {
        when(sensorRepository.existsByName("Station-01")).thenReturn(true);

        assertThrows(ConflictException.class, () -> sensorService.createNewSensorStation(sampleStation));
        verify(sensorRepository, never()).save(any());
    }

    // --- updateExistingSensor ---

    @Test
    void testThatUpdateExistingSensorUpdatesDetailsAndPublishesEvent() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any(SensorStation.class))).thenAnswer(i -> i.getArgument(0));

        SensorStation patch = new SensorStation();
        patch.setName("Updated-Name");
        patch.setStatus(DeviceStatus.ONLINE);
        patch.setLastHeartBeat(LocalDateTime.now());

        SensorStation result = sensorService.updateExistingSensor(stationId, patch);

        assertEquals("Updated-Name", result.getName());
        assertEquals(DeviceStatus.ONLINE, result.getStatus());
        verify(eventPublisher).publishEvent(any(NotifyRaspberryCommand.class));
        verify(sensorRepository).save(any());
    }

    @Test
    void testThatUpdateExistingSensorChangesRoomCorrectly() {
        UUID newRoomId = UUID.randomUUID();
        RoomMonitoring newRoom = new RoomMonitoring();
        newRoom.setRoomId(newRoomId);

        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any(SensorStation.class))).thenAnswer(i -> i.getArgument(0));

        SensorStation patch = new SensorStation();
        patch.setRoomMonitoring(newRoom);

        SensorStation result = sensorService.updateExistingSensor(stationId, patch);

        assertEquals(newRoom, result.getRoomMonitoring());
        assertEquals(result, newRoom.getSensorStation());
        verify(monitoringRepository).save(newRoom);
    }

    @Test
    void testThatUpdateExistingSensorThrowsConflictOnDuplicateName() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.existsByName("Other-Station")).thenReturn(true);

        SensorStation patch = new SensorStation();
        patch.setName("Other-Station");

        assertThrows(ConflictException.class, () -> sensorService.updateExistingSensor(stationId, patch));
    }

    @Test
    void testThatUpdateExistingSensorAllowsSameNameWithoutCheck() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any(SensorStation.class))).thenAnswer(i -> i.getArgument(0));

        SensorStation patch = new SensorStation();
        patch.setName("Station-01"); // Name is identical to sampleStation

        sensorService.updateExistingSensor(stationId, patch);

        verify(sensorRepository, never()).existsByName(anyString());
    }

    @Test
    void testThatUpdateExistingSensorThrowsNotFoundWhenStationMissing() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sensorService.updateExistingSensor(stationId, new SensorStation()));
    }

    // --- getSpecificSensor ---

    @Test
    void testThatGetSpecificSensorReturnsSensorWhenExists() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        SensorStation result = sensorService.getSpecificSensor(stationId);

        assertNotNull(result);
        assertEquals(stationId, result.getId());
    }

    @Test
    void testThatGetSpecificSensorThrowsNotFoundWhenMissing() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sensorService.getSpecificSensor(stationId));
    }

    // --- deleteById ---

    @Test
    void testThatDeleteByIdCleansUpRoomAndPublishesEvent() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        sensorService.deleteById(stationId);

        assertNull(sampleRoom.getSensorStation());
        verify(monitoringRepository).save(sampleRoom);
        verify(eventPublisher).publishEvent(any(NotifyRaspberryCommand.class));
        verify(sensorRepository).deleteById(stationId);
    }

    @Test
    void testThatDeleteByIdSucceedsEvenIfNoRoomLinked() {
        sampleStation.setRoomMonitoring(null);
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        sensorService.deleteById(stationId);

        verify(monitoringRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(sensorRepository).deleteById(stationId);
    }

    @Test
    void testThatDeleteByIdThrowsNotFoundWhenMissing() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sensorService.deleteById(stationId));
        verify(sensorRepository, never()).deleteById(any());
    }
}