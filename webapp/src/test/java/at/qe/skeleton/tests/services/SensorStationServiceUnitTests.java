package at.qe.skeleton.tests.services;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.SensorStationRepository;
import at.qe.skeleton.services.LiveDataService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorStationServiceUnitTests {

    @Mock private SensorStationRepository sensorRepository;
    @Mock private NotificationClient notificationClient;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private LiveDataService liveDataService;

    @InjectMocks
    private SensorStationServiceImpl sensorService;

    private SensorStation sampleStation;
    private RoomMonitoring sampleRoom;
    private RaspberryPi samplePi;
    private UUID stationId;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        stationId = UUID.randomUUID();
        roomId = UUID.randomUUID();

        samplePi = new RaspberryPi();
        samplePi.setId(UUID.randomUUID());
        samplePi.setIp("192.168.1.1");   // ← needed — NotifyRaspberryCommand extracts at construction
        samplePi.setPort(8080);
        samplePi.setName("Pi-01");

        sampleRoom = new RoomMonitoring();
        sampleRoom.setRoomId(roomId);
        sampleRoom.setRoomNumber("ENG-101");

        sampleStation = new SensorStation();
        sampleStation.setReadId(stationId);
        sampleStation.setWriteId(UUID.randomUUID());
        sampleStation.setName("Station-01");
        sampleStation.setRoomMonitoring(sampleRoom);

        sampleRoom.setSensorStations(new ArrayList<>(List.of(sampleStation)));
    }

    // --- getAllSensorStations ---

    @Test
    void testThatGetAllSensorStationsDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SensorStation> page = new PageImpl<>(List.of(sampleStation));
        when(sensorRepository.findAll(pageable)).thenReturn(page);

        Page<SensorStation> result = sensorService.getAllSensorStations(pageable);

        assertEquals(1, result.getTotalElements());
        verify(sensorRepository).findAll(pageable);
    }

    // --- createNewSensorStation ---

    @Test
    void testThatCreateNewSensorStationSucceedsWithoutNotifyingWhenNoPiLinked() {
        when(sensorRepository.existsByName("Station-01")).thenReturn(false);
        when(sensorRepository.save(sampleStation)).thenReturn(sampleStation);

        SensorStation result = sensorService.createNewSensorStation(sampleStation);

        assertNotNull(result);
        verify(sensorRepository).save(sampleStation);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatCreateNewSensorStationNotifiesPiWhenLinked() {
        sampleRoom.setRaspberryPi(samplePi);

        when(sensorRepository.existsByName("Station-01")).thenReturn(false);
        when(sensorRepository.save(sampleStation)).thenReturn(sampleStation);

        sensorService.createNewSensorStation(sampleStation);

        verify(eventPublisher, times(1)).publishEvent(any(NotifyRaspberryCommand.class));
    }

    @Test
    void testThatCreateNewSensorStationDoesNotNotifyWhenRoomIsNull() {
        sampleStation.setRoomMonitoring(null);

        when(sensorRepository.existsByName("Station-01")).thenReturn(false);
        when(sensorRepository.save(sampleStation)).thenReturn(sampleStation);

        sensorService.createNewSensorStation(sampleStation);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatCreateNewSensorStationThrowsConflictWhenNameExists() {
        when(sensorRepository.existsByName("Station-01")).thenReturn(true);

        assertThrows(ConflictException.class, () -> sensorService.createNewSensorStation(sampleStation));
        verify(sensorRepository, never()).save(any());
    }

    // --- updateExistingSensor ---

    @Test
    void testThatUpdateExistingSensorUpdatesScalarFieldsWithoutNotifyingAndSendsToWebsocketStatus() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SensorStation patch = new SensorStation();
        patch.setStatus(DeviceStatus.ONLINE);
        patch.setLastHeartBeat(LocalDateTime.now());

        SensorStation result = sensorService.updateExistingSensor(stationId, patch);

        assertEquals(DeviceStatus.ONLINE, result.getStatus());
        verify(eventPublisher, never()).publishEvent(any());
        verify(liveDataService, times(1)).pushConnectionStatusArduino(stationId, DeviceStatus.ONLINE);
    }

    @Test
    void testThatUpdateExistingSensorNotifiesNewPiWhenRoomChangedAndNewPiLinked() {
        // old room has no Pi, new room has Pi
        RoomMonitoring newRoom = new RoomMonitoring();
        newRoom.setRoomId(UUID.randomUUID());
        newRoom.setRaspberryPi(samplePi);

        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SensorStation patch = new SensorStation();
        patch.setRoomMonitoring(newRoom);

        sensorService.updateExistingSensor(stationId, patch);

        // one SENSOR_ADD to the new room's Pi; old room has no Pi so no SENSOR_DELETE
        verify(eventPublisher, times(1)).publishEvent(any(NotifyRaspberryCommand.class));
        verify(liveDataService, never()).pushConnectionStatusArduino(any(UUID.class), any(DeviceStatus.class));
    }

    @Test
    void testThatUpdateExistingSensorNotifiesBothPisWhenBothRoomsHavePi() {
        sampleRoom.setRaspberryPi(samplePi); // old room has Pi

        RaspberryPi newPi = new RaspberryPi();
        newPi.setId(UUID.randomUUID());
        newPi.setIp("192.168.1.2");
        newPi.setPort(9090);
        newPi.setName("Pi-02");

        RoomMonitoring newRoom = new RoomMonitoring();
        newRoom.setRoomId(UUID.randomUUID());
        newRoom.setRaspberryPi(newPi);

        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SensorStation patch = new SensorStation();
        patch.setRoomMonitoring(newRoom);

        sensorService.updateExistingSensor(stationId, patch);

        // SENSOR_ADD to new Pi + SENSOR_DELETE to old Pi
        verify(eventPublisher, times(2)).publishEvent(any(NotifyRaspberryCommand.class));
        verify(liveDataService, never()).pushConnectionStatusArduino(any(UUID.class), any(DeviceStatus.class));
    }

    @Test
    void testThatUpdateExistingSensorDoesNotNotifyWhenRoomIsUnchanged() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SensorStation patch = new SensorStation();
        patch.setRoomMonitoring(sampleRoom); // same roomId — no notify

        sensorService.updateExistingSensor(stationId, patch);

        verify(eventPublisher, never()).publishEvent(any());
        verify(liveDataService, never()).pushConnectionStatusArduino(any(UUID.class), any(DeviceStatus.class));
    }

    @Test
    void testThatUpdateExistingSensorNotifiesOnNameChangeWhenPiLinked() {
        sampleRoom.setRaspberryPi(samplePi);

        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.existsByName("New-Name")).thenReturn(false);
        when(sensorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SensorStation patch = new SensorStation();
        patch.setName("New-Name");

        sensorService.updateExistingSensor(stationId, patch);

        // SENSOR_ADD + SENSOR_DELETE for name change
        verify(eventPublisher, times(2)).publishEvent(any(NotifyRaspberryCommand.class));
        verify(liveDataService, never()).pushConnectionStatusArduino(any(UUID.class), any(DeviceStatus.class));
    }

    @Test
    void testThatUpdateExistingSensorSkipsNotifyOnNameChangeWhenNoPiLinked() {
        // sampleRoom has no Pi
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.existsByName("New-Name")).thenReturn(false);
        when(sensorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SensorStation patch = new SensorStation();
        patch.setName("New-Name");

        sensorService.updateExistingSensor(stationId, patch);

        verify(eventPublisher, never()).publishEvent(any());
        verify(liveDataService, never()).pushConnectionStatusArduino(any(UUID.class), any(DeviceStatus.class));
    }

    @Test
    void testThatUpdateExistingSensorThrowsConflictOnDuplicateName() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.existsByName("Other-Station")).thenReturn(true);

        SensorStation patch = new SensorStation();
        patch.setName("Other-Station");

        assertThrows(ConflictException.class, () -> sensorService.updateExistingSensor(stationId, patch));
        verify(sensorRepository, never()).save(any());
        verify(liveDataService, never()).pushConnectionStatusArduino(any(UUID.class), any(DeviceStatus.class));
    }

    @Test
    void testThatUpdateExistingSensorThrowsNotFoundWhenStationMissing() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> sensorService.updateExistingSensor(stationId, new SensorStation()));
    }

    // --- getSpecificSensor ---

    @Test
    void testThatGetSpecificSensorReturnsSensorWhenExists() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        SensorStation result = sensorService.getSpecificSensor(stationId);

        assertEquals(sampleStation, result);
    }

    @Test
    void testThatGetSpecificSensorThrowsNotFoundWhenMissing() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sensorService.getSpecificSensor(stationId));
    }

    // --- disconnectFromRoom ---

    @Test
    void testThatDisconnectFromRoomClearsMonitoringAndNotifiesPi() {
        sampleRoom.setRaspberryPi(samplePi);
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SensorStation result = sensorService.disconnectFromRoom(stationId);

        assertNull(result.getRoomMonitoring());
        verify(eventPublisher, times(1)).publishEvent(any(NotifyRaspberryCommand.class));
    }

    @Test
    void testThatDisconnectFromRoomDoesNotNotifyWhenNoPiLinked() {
        // sampleRoom has no Pi
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        sensorService.disconnectFromRoom(stationId);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatDisconnectFromRoomDoesNotNotifyWhenNoRoomLinked() {
        sampleStation.setRoomMonitoring(null);
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        sensorService.disconnectFromRoom(stationId);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatDisconnectFromRoomThrowsNotFoundWhenMissing() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sensorService.disconnectFromRoom(stationId));
    }

    // --- deleteById ---

    @Test
    void testThatDeleteByIdDeletesSensorAndPublishesEventWhenPiLinked() {
        sampleRoom.setRaspberryPi(samplePi);
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        sensorService.deleteById(stationId);

        verify(sensorRepository).deleteById(stationId);
        verify(eventPublisher, times(1)).publishEvent(any(NotifyRaspberryCommand.class));
    }

    @Test
    void testThatDeleteByIdDoesNotPublishEventWhenNoPiLinked() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        sensorService.deleteById(stationId);

        verify(sensorRepository).deleteById(stationId);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatDeleteByIdDoesNotPublishEventWhenNoRoomLinked() {
        sampleStation.setRoomMonitoring(null);
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        sensorService.deleteById(stationId);

        verify(sensorRepository).deleteById(stationId);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatDeleteByIdThrowsNotFoundWhenStationMissing() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sensorService.deleteById(stationId));
        verify(sensorRepository, never()).deleteById(any());
    }

    // --- retryConnection ---

    @Test
    void testThatRetryConnectionThrowsNotFoundWhenStationMissing() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sensorService.retryConnection(stationId));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatRetryConnectionDoesNotPublishEventWhenNoRoomLinked() {
        sampleStation.setRoomMonitoring(null);
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        assertDoesNotThrow(() -> sensorService.retryConnection(stationId));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatRetryConnectionDoesNotPublishEventWhenNoPiLinked() {
        // sampleRoom has no Pi
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        assertDoesNotThrow(() -> sensorService.retryConnection(stationId));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatRetryConnectionPublishesEventWhenPiLinked() {
        sampleRoom.setRaspberryPi(samplePi);
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        assertDoesNotThrow(() -> sensorService.retryConnection(stationId));
        verify(eventPublisher, times(1)).publishEvent(any(NotifyRaspberryCommand.class));
    }
}