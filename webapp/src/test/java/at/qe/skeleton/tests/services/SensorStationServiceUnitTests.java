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
public class SensorStationServiceUnitTests {

    @Mock private SensorStationRepository sensorRepository;
    @Mock private NotificationClient notificationClient;
    @Mock private ApplicationEventPublisher eventPublisher;

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

        sampleRoom = new RoomMonitoring();
        sampleRoom.setRoomId(roomId);

        sampleStation = new SensorStation();
        sampleStation.setReadId(stationId);
        sampleStation.setWriteId(UUID.randomUUID());
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

        assertNotNull(result);
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

        verify(eventPublisher).publishEvent(any(NotifyRaspberryCommand.class));
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
    void testThatUpdateExistingSensorUpdatesScalarFields() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SensorStation patch = new SensorStation();
        patch.setName("Updated-Name");
        patch.setStatus(DeviceStatus.ONLINE);
        patch.setLastHeartBeat(LocalDateTime.now());

        SensorStation result = sensorService.updateExistingSensor(stationId, patch);

        assertEquals("Updated-Name", result.getName());
        assertEquals(DeviceStatus.ONLINE, result.getStatus());
        // no room change, so no event
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatUpdateExistingSensorNotifiesNewPiWhenRoomChangedAndPiLinked() {
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
    }

    @Test
    void testThatUpdateExistingSensorNotifiesBothPisWhenBothRoomsHavePi() {
        sampleRoom.setRaspberryPi(samplePi);

        RaspberryPi newPi = new RaspberryPi();
        newPi.setId(UUID.randomUUID());
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
    }

    @Test
    void testThatUpdateExistingSensorDoesNotNotifyWhenRoomIsUnchanged() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SensorStation patch = new SensorStation();
        patch.setRoomMonitoring(sampleRoom); // same room — equals() returns true, no notify

        sensorService.updateExistingSensor(stationId, patch);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatUpdateExistingSensorSkipsNameConflictCheckWhenNameIsUnchanged() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SensorStation patch = new SensorStation();
        patch.setName("Station-01");

        sensorService.updateExistingSensor(stationId, patch);

        verify(sensorRepository, never()).existsByName(anyString());
    }

    @Test
    void testThatUpdateExistingSensorThrowsConflictOnDuplicateName() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.existsByName("Other-Station")).thenReturn(true);

        SensorStation patch = new SensorStation();
        patch.setName("Other-Station");

        assertThrows(ConflictException.class, () -> sensorService.updateExistingSensor(stationId, patch));
        verify(sensorRepository, never()).save(any());
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

        assertEquals(sampleStation, result);
    }

    @Test
    void testThatGetSpecificSensorThrowsNotFoundWhenMissing() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sensorService.getSpecificSensor(stationId));
    }

    // --- deleteById ---

    @Test
    void testThatDeleteByIdDeletesSensorAndPublishesEventWhenPiLinked() {
        sampleRoom.setRaspberryPi(samplePi);
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        sensorService.deleteById(stationId);

        verify(sensorRepository).deleteById(stationId);
        verify(eventPublisher).publishEvent(any(NotifyRaspberryCommand.class));
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

    @Test
    void testThatRetryConnectionThrowsNotFoundExceptionIfNoSensorWasFound() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> sensorService.retryConnection(stationId));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatRetryConnectionDoesNotTriggerEventIfSensorDoesNotHaveARoom() {
        sampleStation.setRoomMonitoring(null);
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        assertDoesNotThrow(() -> sensorService.retryConnection(stationId));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatRetryConnectionTriggersEventPublishingIfRoomWasFound() {
        sampleRoom.setRaspberryPi(samplePi);
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        assertDoesNotThrow(() -> sensorService.retryConnection(stationId));
        verify(eventPublisher, times(1)).publishEvent(any(NotifyRaspberryCommand.class));
    }
}