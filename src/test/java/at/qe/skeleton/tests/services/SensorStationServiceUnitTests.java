package at.qe.skeleton.tests.services;

import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.SensorStationRepository;
import at.qe.skeleton.services.impl.SensorStationServiceImpl;
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

        sampleStation = new SensorStation();
        sampleStation.setId(stationId);
        sampleStation.setName("Station A");

        sampleRoom = new RoomMonitoring();
        sampleRoom.setRoomId(roomId);

        sampleStation.setRoomMonitoring(sampleRoom);
        sampleRoom.setSensorStation(sampleStation);
    }

    @Test
    void testThatGetAllSensorStationsShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SensorStation> page = new PageImpl<>(List.of(sampleStation));
        when(sensorRepository.findAll(pageable)).thenReturn(page);

        Page<SensorStation> result = sensorService.getAllSensorStations(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(sensorRepository, times(1)).findAll(pageable);
    }

    @Test
    void testThatCreateNewSensorStationSucceedsAndLinksRoom() {
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(sensorRepository.existsByName(anyString())).thenReturn(false);
        when(sensorRepository.save(any(SensorStation.class))).thenReturn(sampleStation);

        SensorStation result = sensorService.createNewSensorStation(sampleStation, roomId);

        assertNotNull(result);
        assertEquals(sampleStation, sampleRoom.getSensorStation());
        verify(sensorRepository).save(sampleStation);
        verify(monitoringRepository).save(sampleRoom);
    }

    @Test
    void testThatCreateNewSensorStationThrowsConflictIfNameExists() {
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(sensorRepository.existsByName("Station A")).thenReturn(true);

        assertThrows(ConflictException.class, () -> sensorService.createNewSensorStation(sampleStation, roomId));
        verify(sensorRepository, never()).save(any());
    }

    @Test
    void testThatUpdateExistingSensorShouldChangeRoomIfDifferent() {
        UUID newRoomId = UUID.randomUUID();
        RoomMonitoring newRoom = new RoomMonitoring();
        newRoom.setRoomId(newRoomId);

        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(monitoringRepository.findById(newRoomId)).thenReturn(Optional.of(newRoom));
        when(sensorRepository.save(any(SensorStation.class))).thenAnswer(i -> i.getArgument(0));

        SensorStation updatedData = new SensorStation();
        updatedData.setName("New Name");

        SensorStation result = sensorService.updateExistingSensor(stationId, updatedData, newRoomId);

        assertEquals("New Name", result.getName());
        assertEquals(newRoom, result.getRoomMonitoring());
        assertEquals(result, newRoom.getSensorStation());
        verify(monitoringRepository).save(newRoom);
    }

    @Test
    void testThatUpdateExistingSensorShouldThrowConflictIfNewNameExists() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.existsByName("Taken Name")).thenReturn(true);

        SensorStation updatedData = new SensorStation();
        updatedData.setName("Taken Name");

        assertThrows(ConflictException.class, () -> sensorService.updateExistingSensor(stationId, updatedData, roomId));
    }

    @Test
    void testThatUpdateExistingSensorUpdatesStatusAndHeartbeat() {
        LocalDateTime now = LocalDateTime.now();
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(sensorRepository.save(any(SensorStation.class))).thenAnswer(i -> i.getArgument(0));

        SensorStation updatedData = new SensorStation();
        updatedData.setStatus(DeviceStatus.ONLINE);
        updatedData.setLastHeartBeat(now);

        SensorStation result = sensorService.updateExistingSensor(stationId, updatedData, roomId);

        assertEquals(DeviceStatus.ONLINE, result.getStatus());
        assertEquals(now, result.getLastHeartBeat());
    }

    @Test
    void testThatGetSpecificSensorReturnsSensorWhenExists() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        SensorStation result = sensorService.getSpecificSensor(stationId);

        assertEquals(sampleStation, result);
    }

    @Test
    void testThatDeleteByIdSucceedsAndUnlinksRoom() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        sensorService.deleteById(stationId);

        assertNull(sampleRoom.getSensorStation());
        verify(monitoringRepository).save(sampleRoom);
        verify(sensorRepository).deleteById(stationId);
    }

    @Test
    void testThatDeleteByIdThrowsNotFoundWhenMissing() {
        when(sensorRepository.findById(stationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sensorService.deleteById(stationId));
    }
}