package at.qe.skeleton.tests.services;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.PiConfigDTO;
import at.qe.skeleton.dtos.UpdateType;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.NotifyDeadLetter;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.NotifyDeadLetterRepository;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

        // Link Pi and Room to simulate a fully established relationship
        samplePi.setRoomMonitoring(sampleRoom);
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
    void testThatCreateNewRaspberrySucceedsAndLinksRoom() {
        when(raspberryPiRepository.existsByName("Pi-01")).thenReturn(false);
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(raspberryPiRepository.save(any(RaspberryPi.class))).thenReturn(samplePi);

        RaspberryPi newPi = new RaspberryPi();
        newPi.setName("Pi-01");
        newPi.setIp("192.168.1.100");

        RaspberryPi result = raspberryService.createNewRaspberry(newPi, roomId);

        assertNotNull(result);
        assertEquals(newPi, sampleRoom.getRaspberryPi()); // Room link established
        verify(monitoringRepository).save(sampleRoom);
    }

    @Test
    void testThatCreateNewRaspberryThrowsNotFoundIfRoomWasNotFound() {
        when(raspberryPiRepository.existsByName("Pi-01")).thenReturn(false);
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.createNewRaspberry(samplePi, roomId));
        verify(raspberryPiRepository, never()).save(any());
    }

    @Test
    void testThatCreateNewRaspberryThrowsConflictOnDuplicateName() {
        when(raspberryPiRepository.existsByName("Pi-01")).thenReturn(true);

        assertThrows(ConflictException.class, () -> raspberryService.createNewRaspberry(samplePi, roomId));
    }
    @Test
    void testThatUpdateRaspberryByIdUpdatesOnlyProvidedFields() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(raspberryPiRepository.save(any(RaspberryPi.class))).thenAnswer(i -> i.getArgument(0));

        RaspberryPi patchData = new RaspberryPi();
        patchData.setFrequency(9999);

        RaspberryPi result = raspberryService.updateRaspberryById(piId, patchData, null);

        assertEquals(9999, result.getFrequency());
        assertEquals("Pi-01", result.getName());
        assertEquals("192.168.1.100", result.getIp());
        verify(raspberryPiRepository, never()).existsByName(anyString());
        verify(raspberryPiRepository, never()).existsByIp(anyString());
        verify(monitoringRepository, never()).findById(any());
    }

    @Test
    void testThatUpdateRaspberryByIdSkipsConflictCheckWhenNameIsUnchanged() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(raspberryPiRepository.save(any(RaspberryPi.class))).thenAnswer(i -> i.getArgument(0));

        RaspberryPi patchData = new RaspberryPi();
        patchData.setName("Pi-01");

        RaspberryPi result = raspberryService.updateRaspberryById(piId, patchData, null);

        assertEquals("Pi-01", result.getName());
        verify(raspberryPiRepository, never()).existsByName(anyString());
    }

    @Test
    void testThatUpdateRaspberryByIdSkipsConflictCheckWhenIpIsUnchanged() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(raspberryPiRepository.save(any(RaspberryPi.class))).thenAnswer(i -> i.getArgument(0));

        RaspberryPi patchData = new RaspberryPi();
        patchData.setIp("192.168.1.100");

        RaspberryPi result = raspberryService.updateRaspberryById(piId, patchData, null);

        assertEquals("192.168.1.100", result.getIp());
        verify(raspberryPiRepository, never()).existsByIp(anyString());
    }

    @Test
    void testThatUpdateRaspberryByIdDoesNotTouchRoomWhenRoomIdIsUnchanged() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(raspberryPiRepository.save(any(RaspberryPi.class))).thenAnswer(i -> i.getArgument(0));

        RaspberryPi patchData = new RaspberryPi();
        patchData.setFrequency(1234);

        raspberryService.updateRaspberryById(piId, patchData, roomId);

        verify(monitoringRepository, never()).findById(any());
        verify(monitoringRepository, never()).save(any());
    }

    @Test
    void testThatUpdateRaspberryByIdAssignsRoomWhenCurrentlyNull() {
        samplePi.setRoomMonitoring(null);

        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(monitoringRepository.findById(roomId)).thenReturn(Optional.of(sampleRoom));
        when(raspberryPiRepository.save(any(RaspberryPi.class))).thenAnswer(i -> i.getArgument(0));

        RaspberryPi patchData = new RaspberryPi();
        patchData.setFrequency(1000);

        RaspberryPi result = raspberryService.updateRaspberryById(piId, patchData, roomId);

        assertEquals(sampleRoom, result.getRoomMonitoring());
        verify(monitoringRepository).save(sampleRoom);
    }

    @Test
    void testThatUpdateRaspberryByIdThrowsNotFoundWhenRoomDoesNotExist() {
        UUID newRoomId = UUID.randomUUID();
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(monitoringRepository.findById(newRoomId)).thenReturn(Optional.empty());

        RaspberryPi patchData = new RaspberryPi();

        assertThrows(NotFoundException.class, () -> raspberryService.updateRaspberryById(piId, patchData, newRoomId));
    }

    @Test
    void testThatDeleteRaspberrySucceeds() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        raspberryService.deleteRaspberry(piId);

        assertNull(sampleRoom.getRaspberryPi());
        verify(monitoringRepository).save(sampleRoom);
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

        // When Redis logic is added, one will mock the RedisTemplate here
        int occupancy = raspberryService.getOccupancyFromRedis(piId);

        assertEquals(10, occupancy);
    }

    @Test
    void testThatGetConfigForRaspberryNavigatesGraphCorrectly() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        PiConfigDTO config = raspberryService.getConfigForRaspberry(piId);

        assertNotNull(config);
        assertEquals(5000, config.frequency());
        assertEquals(sensorId, config.sensor());
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

        // initial SETUP + one replayed non-SETUP letter = 2 events
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

        // only the initial SETUP event, the dead letter one is filtered
        verify(eventPublisher, times(1)).publishEvent(any(NotifyRaspberryCommand.class));
    }

    @Test
    void testThatRetryConnectionThrowsNotFoundWhenPiMissing() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.retryConnection(piId));
        verify(eventPublisher, never()).publishEvent(any());
        verify(deadLetterRepository, never()).findAll();
    }

    @Test
    void testThatUpdateRaspberryByIdThrowsConflictOnDuplicateName() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(raspberryPiRepository.existsByName("Pi-02")).thenReturn(true);

        RaspberryPi patchData = new RaspberryPi();
        patchData.setName("Pi-02");

        assertThrows(ConflictException.class, () -> raspberryService.updateRaspberryById(piId, patchData, null));
        verify(raspberryPiRepository, never()).save(any());
    }

    @Test
    void testThatUpdateRaspberryByIdReassignsRoomWhenDifferentRoomIdProvided() {
        UUID newRoomId = UUID.randomUUID();
        RoomMonitoring newRoom = new RoomMonitoring();
        newRoom.setRoomId(newRoomId);

        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));
        when(monitoringRepository.findById(newRoomId)).thenReturn(Optional.of(newRoom));
        when(raspberryPiRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RaspberryPi result = raspberryService.updateRaspberryById(piId, new RaspberryPi(), newRoomId);

        assertEquals(newRoom, result.getRoomMonitoring());
        verify(monitoringRepository).save(newRoom);
    }

    @Test
    void testThatGetConfigForRaspberryReturnNullSensorWhenNotLinked() {
        sampleRoom.setSensorStation(null);
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.of(samplePi));

        PiConfigDTO config = raspberryService.getConfigForRaspberry(piId);

        assertNotNull(config);
        assertNull(config.sensor());
    }

    @Test
    void testThatGetConfigForRaspberryThrowsNotFoundWhenPiMissing() {
        when(raspberryPiRepository.findById(piId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> raspberryService.getConfigForRaspberry(piId));
    }
}