package at.qe.skeleton.tests.services;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.PiConfigDTO;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.mappers.LimitMapper;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.RoomOccupancy;
import at.qe.skeleton.model.SensorStation;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RaspberryServiceUnitTests {

    @Mock private RaspberryPiRepository raspberryPiRepository;
    @Mock private RoomMonitoringRepository monitoringRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private NotificationClient notificationClient;
    @Mock private RoomOccupancyRepository occupancyRepository;
    @Mock private LimitMapper limitMapper;

    @InjectMocks
    private RaspberryServiceImpl raspberryService;

    private RaspberryPi samplePi;
    private RoomMonitoring sampleRoom;
    private SensorStation sampleSensor;
    private UUID piId;
    private UUID roomId;
    private UUID sensorReadId;
    private UUID sensorWriteId;

    @BeforeEach
    void setUp() {
        piId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        sensorReadId = UUID.randomUUID();
        sensorWriteId = UUID.randomUUID();

        sampleSensor = new SensorStation();
        sampleSensor.setName("Sensor-01");
        sampleSensor.setReadId(sensorReadId);
        sampleSensor.setWriteId(sensorWriteId);

        sampleRoom = new RoomMonitoring();
        sampleRoom.setRoomId(roomId);
        sampleRoom.setSensorStations(new ArrayList<>(List.of(sampleSensor)));

        samplePi = new RaspberryPi();
        samplePi.setId(piId);
        samplePi.setName("Pi-01");
        samplePi.setIp("192.168.1.100");
        samplePi.setPort(8080);
        samplePi.setFrequency(5000);
        samplePi.setRoomMonitoring(sampleRoom);
        sampleRoom.setRaspberryPi(samplePi);
        sampleSensor.setRoomMonitoring(sampleRoom);
    }

    @Test
    void testThatGetAllRaspberriesDelegatesToRepository() {
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
        RaspberryPi newPi = new RaspberryPi();
        newPi.setName("Pi-02");
        newPi.setIp("192.168.1.101");
        newPi.setPort(9090);

        when(raspberryPiRepository.existsByName("Pi-02")).thenReturn(false);
        when(raspberryPiRepository.existsByIpAndPort("192.168.1.101", 9090)).thenReturn(false);
        when(raspberryPiRepository.save(newPi)).thenReturn(newPi);

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
    void testThatCreateNewRaspberryThrowsConflictOnDuplicateIpAndPort() {
        when(raspberryPiRepository.existsByName("Pi-01")).thenReturn(false);
        when(raspberryPiRepository.existsByIpAndPort("192.168.1.100", 8080)).thenReturn(true);

        assertThrows(ConflictException.class, () -> raspberryService.createNewRaspberry(samplePi));
        verify(raspberryPiRepository, never()).save(any());
    }

    @Test
    void testThatUpdateRaspberryByIdUpdatesOnlyProvidedFields() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(raspberryPiRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RaspberryPi patch = new RaspberryPi();
        patch.setFrequency(9999);

        RaspberryPi result = raspberryService.updateRaspberryById(piId, patch);

        assertEquals(9999, result.getFrequency());
        assertEquals("Pi-01", result.getName());
        assertEquals("192.168.1.100", result.getIp());
        verify(raspberryPiRepository, never()).existsByName(anyString());
        verify(raspberryPiRepository, never()).existsByIpAndPort(anyString(), anyInt());
    }

    @Test
    void testThatUpdateRaspberryByIdSkipsNameConflictCheckWhenNameIsUnchanged() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(raspberryPiRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RaspberryPi patch = new RaspberryPi();
        patch.setName("Pi-01");

        RaspberryPi result = raspberryService.updateRaspberryById(piId, patch);

        assertEquals("Pi-01", result.getName());
        verify(raspberryPiRepository, never()).existsByName(anyString());
    }

    @Test
    void testThatUpdateRaspberryByIdThrowsConflictOnDuplicateName() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(raspberryPiRepository.existsByName("Pi-02")).thenReturn(true);

        RaspberryPi patch = new RaspberryPi();
        patch.setName("Pi-02");

        assertThrows(ConflictException.class, () -> raspberryService.updateRaspberryById(piId, patch));
        verify(raspberryPiRepository, never()).save(any());
    }

    @Test
    void testThatUpdateRaspberryByIdThrowsNotFoundWhenMissing() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.updateRaspberryById(piId, new RaspberryPi()));
    }

    @Test
    void testThatDeleteRaspberryUnlinksRoomAndDeletesPi() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        raspberryService.deleteRaspberry(piId);

        assertNull(sampleRoom.getRaspberryPi());
        verify(monitoringRepository).save(sampleRoom);
        verify(raspberryPiRepository).deleteById(piId);
    }

    @Test
    void testThatDeleteRaspberryDoesNothingWhenPiNotFound() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.empty());

        raspberryService.deleteRaspberry(piId);

        verify(raspberryPiRepository, never()).deleteById(any());
        verifyNoInteractions(monitoringRepository);
    }

    @Test
    void testThatDeleteRaspberrySkipsMonitoringUnlinkWhenNoRoomAssigned() {
        samplePi.setRoomMonitoring(null);
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        raspberryService.deleteRaspberry(piId);

        verify(raspberryPiRepository).deleteById(piId);
        verifyNoInteractions(monitoringRepository);
    }

    @Test
    void testThatGetOccupancyFromRedisReturnsList() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(occupancyRepository.findAllById(List.of(roomId.toString())))
                .thenReturn(List.of(RoomOccupancy.builder().roomId(roomId).build()));

        List<RoomOccupancy> result = raspberryService.getOccupancyFromRedis(piId);

        assertEquals(1, result.size());
    }

    @Test
    void testThatGetOccupancyFromRedisReturnsEmptyWhenNoRoomAssigned() {
        samplePi.setRoomMonitoring(null);
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        List<RoomOccupancy> result = raspberryService.getOccupancyFromRedis(piId);

        assertTrue(result.isEmpty());
        verifyNoInteractions(occupancyRepository);
    }

    @Test
    void testThatGetOccupancyFromRedisThrowsNotFoundWhenPiMissing() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.getOccupancyFromRedis(piId));
    }

    @Test
    void testThatGetConfigForRaspberryReturnsFrequencyAndSensors() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        PiConfigDTO config = raspberryService.getConfigForRaspberry(piId);

        assertNotNull(config);
        assertEquals(5000, config.frequency());
        assertEquals(roomId, config.roomId());
        assertEquals(piId, config.raspberryId());
        assertTrue(config.sensors().stream()
                .anyMatch(s -> s.readId().equals(sensorReadId) && s.writeId().equals(sensorWriteId)));
    }

    @Test
    void testThatGetConfigForRaspberryReturnsEmptySensorsWhenNoneLinked() {
        sampleRoom.setSensorStations(new ArrayList<>());
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        PiConfigDTO config = raspberryService.getConfigForRaspberry(piId);

        assertTrue(config.sensors().isEmpty());
    }

    @Test
    void testThatGetConfigForRaspberryThrowsNotFoundWhenPiMissing() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.getConfigForRaspberry(piId));
    }

    @Test
    void testThatRetryConnectionPublishesEventWhenPiExists() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        raspberryService.retryConnection(piId);

        verify(eventPublisher).publishEvent(any(NotifyRaspberryCommand.class));
    }

    @Test
    void testThatRetryConnectionThrowsNotFoundWhenPiMissing() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.retryConnection(piId));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testThatAddNewRoomSucceeds() {
        RaspberryPi piWithNoRoom = new RaspberryPi();
        piWithNoRoom.setId(UUID.randomUUID());
        piWithNoRoom.setRoomMonitoring(null);

        RoomMonitoring unassignedRoom = new RoomMonitoring();
        unassignedRoom.setRoomId(UUID.randomUUID());

        when(raspberryPiRepository.findById(piWithNoRoom.getId())).thenReturn(Optional.of(piWithNoRoom));
        when(monitoringRepository.findById(unassignedRoom.getRoomId())).thenReturn(Optional.of(unassignedRoom));
        when(monitoringRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(raspberryPiRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RaspberryPi result = raspberryService.addNewRoom(piWithNoRoom.getId(), unassignedRoom.getRoomId());

        assertNotNull(result);
        assertEquals(piWithNoRoom, unassignedRoom.getRaspberryPi());
        verify(monitoringRepository).save(unassignedRoom);
        verify(raspberryPiRepository).save(piWithNoRoom);
        verify(eventPublisher).publishEvent(any(NotifyRaspberryCommand.class));
    }

    @Test
    void testThatAddNewRoomThrowsConflictWhenPiAlreadyHasRoom() {
        RoomMonitoring anotherRoom = new RoomMonitoring();
        anotherRoom.setRoomId(UUID.randomUUID());

        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(monitoringRepository.findById(anotherRoom.getRoomId())).thenReturn(Optional.of(anotherRoom));

        assertThrows(ConflictException.class, () -> raspberryService.addNewRoom(piId, anotherRoom.getRoomId()));
        verify(monitoringRepository, never()).save(any());
        verify(raspberryPiRepository, never()).save(any());
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
    void testThatRemoveRoomFromRaspberrySucceeds() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(monitoringRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(raspberryPiRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RaspberryPi result = raspberryService.removeRoomFromRaspberry(piId, roomId);

        assertNotNull(result);
        assertNull(sampleRoom.getRaspberryPi());
        assertNull(result.getRoomMonitoring());
        verify(monitoringRepository).save(sampleRoom);
        verify(raspberryPiRepository).save(samplePi);
        verify(eventPublisher).publishEvent(any(NotifyRaspberryCommand.class));
    }

    @Test
    void testThatRemoveRoomFromRaspberryThrowsNotFoundWhenRoomNotAssignedToPi() {
        RoomMonitoring differentRoom = new RoomMonitoring();
        differentRoom.setRoomId(UUID.randomUUID());

        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(monitoringRepository.findById(differentRoom.getRoomId())).thenReturn(Optional.of(differentRoom));

        assertThrows(NotFoundException.class,
                () -> raspberryService.removeRoomFromRaspberry(piId, differentRoom.getRoomId()));
        verify(monitoringRepository, never()).save(any());
        verify(raspberryPiRepository, never()).save(any());
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