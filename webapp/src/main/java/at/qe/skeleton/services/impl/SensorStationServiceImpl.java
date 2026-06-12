package at.qe.skeleton.services.impl;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import at.qe.skeleton.dtos.UpdateType;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.SensorStationRepository;
import at.qe.skeleton.services.LiveDataService;
import at.qe.skeleton.services.SensorStationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of {@link SensorStationService} managing the lifecycle of
 * {@link SensorStation} entities. Every structural change (add, rename, room
 * reassignment, disconnect, delete) is propagated to the affected Raspberry Pi(s)
 * via {@link NotifyRaspberryCommand} events. Connection-status changes are pushed
 * to WebSocket subscribers through {@link LiveDataService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SensorStationServiceImpl implements SensorStationService {

    private final SensorStationRepository sensorRepository;
    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher eventPublisher;
    private final LiveDataService liveDataService;

    /**
     * Returns a paginated list of all sensor stations.
     *
     * @param pageable pagination parameters
     * @return page of {@link SensorStation} entities
     */
    @Override
    public Page<SensorStation> getAllSensorStations(Pageable pageable) {
        return sensorRepository.findAll(pageable);
    }

    /**
     * Registers a new sensor station. If the station is already assigned to a room
     * with a Raspberry Pi, the Pi is notified of the new sensor via a
     * {@link UpdateType#SENSOR_ADD} command.
     *
     * @param sensorStation the sensor station to create
     * @return the saved {@link SensorStation}
     * @throws ConflictException if a sensor station with the same name already exists
     */
    @Override
    @Transactional
    public SensorStation createNewSensorStation(SensorStation sensorStation) {
        if (sensorRepository.existsByName(sensorStation.getName())) {
            throw new ConflictException("Sensor station with name " + sensorStation.getName() + " already exists.");
        }
        SensorStation station = sensorRepository.save(sensorStation);
        log.info("Added new sensor station '{}' into the system.", station.getName());
        RoomMonitoring monitoring = station.getRoomMonitoring();
        if (monitoring != null && monitoring.getRaspberryPi() != null) {
            log.info("Notifying Raspberry Pi '{}' [{}:{}] about new sensor '{}' being added.", monitoring.getRaspberryPi().getName(),
                    monitoring.getRaspberryPi().getIp(), monitoring.getRaspberryPi().getPort(), sensorStation.getName());
            eventPublisher.publishEvent(
                    new NotifyRaspberryCommand(
                            new StateChangeNotificationDTO(UpdateType.SENSOR_ADD, LocalDateTime.now()),
                            station.getReadId(),
                            station.getWriteId(),
                            monitoring.getRaspberryPi(),
                            notificationClient));
        }
        return station;
    }

    /**
     * Applies a partial update to an existing sensor station. The following side
     * effects apply based on what changed:
     * <ul>
     *   <li><b>Room reassigned</b>: the new room's Pi receives a {@link UpdateType#SENSOR_ADD}
     *       command; the old room's Pi (if any) receives a {@link UpdateType#SENSOR_DELETE}
     *       command.</li>
     *   <li><b>Name changed (no room change)</b>: the room's Pi receives both a
     *       {@link UpdateType#SENSOR_ADD} and a {@link UpdateType#SENSOR_DELETE} command
     *       to force a BLE re-scan.</li>
     *   <li><b>Connection status changed</b>: the new status is pushed to WebSocket
     *       subscribers via {@link LiveDataService#pushConnectionStatusArduino}.</li>
     * </ul>
     *
     * @param id            the UUID of the sensor station to update
     * @param sensorStation a partial {@link SensorStation} carrying the fields to update
     * @return the updated {@link SensorStation}
     * @throws NotFoundException if no sensor station with that ID exists
     * @throws ConflictException if the new name is already taken by another station
     */
    @Override
    @Transactional
    public SensorStation updateExistingSensor(UUID id, SensorStation sensorStation) {
        return sensorRepository.findById(id).map(sensor -> {
            AtomicBoolean notifyRasp = new AtomicBoolean(false);
            AtomicBoolean nameChanged = new AtomicBoolean(false);
            AtomicBoolean connectionStatusChanged = new AtomicBoolean(false);
            RoomMonitoring prevMonitoring = sensor.getRoomMonitoring();
            if (sensorStation.getRoomMonitoring() != null && (sensor.getRoomMonitoring() == null || !sensorStation.getRoomMonitoring().getRoomId().equals(sensor.getRoomMonitoring().getRoomId()))) {
                sensor.setRoomMonitoring(sensorStation.getRoomMonitoring());
                log.info("Pre-saved new room of sensor station: {} -> {}", sensorStation.getRoomMonitoring().getRoomNumber(), sensor.getRoomMonitoring().getRoomNumber());
                notifyRasp.set(true);
            }
            Optional.ofNullable(sensorStation.getName()).ifPresent(name -> {
                if (!name.equals(sensor.getName())) {
                    if (sensorRepository.existsByName(name))
                        throw new ConflictException("Sensor station with name " + sensorStation.getName() + " already exists.");
                    log.info("Pre-saved new name of sensor station: {} -> {}", sensorStation.getName(), name);
                    sensor.setName(name);
                    nameChanged.set(true);
                }
            });
            Optional.ofNullable(sensorStation.getStatus()).ifPresent(status -> {
                if (status.equals(DeviceStatus.OFFLINE)) {
                    log.warn("Pre-saved offline status of sensor station - {}", sensor.getName());
                } else {
                    log.info("Pre-saved online status of sensor station - {}", sensor.getName());
                }
                connectionStatusChanged.set(true);
                sensor.setStatus(status);
            });
            Optional.ofNullable(sensorStation.getLastHeartBeat()).ifPresent(beat -> {
                log.info("Pre-saved last heartbeat of sensor '{}'.", sensor.getName());
                sensor.setLastHeartBeat(beat);
            });
            SensorStation saved = sensorRepository.save(sensor);
            if (!notifyRasp.get() && nameChanged.get() && saved.getRoomMonitoring().getRaspberryPi() != null) {
                    eventPublisher.publishEvent(
                            new NotifyRaspberryCommand(
                                    new StateChangeNotificationDTO(UpdateType.SENSOR_ADD, LocalDateTime.now()),
                                    id,
                                    sensor.getWriteId(),
                                    saved.getRoomMonitoring().getRaspberryPi(),
                                    notificationClient));
                    eventPublisher.publishEvent(
                            new NotifyRaspberryCommand(
                                    new StateChangeNotificationDTO(UpdateType.SENSOR_DELETE, LocalDateTime.now()),
                                    id,
                                    sensor.getWriteId(),
                                    saved.getRoomMonitoring().getRaspberryPi(),
                                    notificationClient));
            }
            if (notifyRasp.get()) {
                if (saved.getRoomMonitoring().getRaspberryPi() != null) {
                    log.info("Notifying Raspberry Pi '{}' [{}:{}] of new sensor...",
                            saved.getRoomMonitoring().getRaspberryPi().getName(),
                            saved.getRoomMonitoring().getRaspberryPi().getIp(),
                            saved.getRoomMonitoring().getRaspberryPi().getPort());
                    eventPublisher.publishEvent(
                            new NotifyRaspberryCommand(
                                    new StateChangeNotificationDTO(UpdateType.SENSOR_ADD, LocalDateTime.now()),
                                    id,
                                    sensor.getWriteId(),
                                    saved.getRoomMonitoring().getRaspberryPi(),
                                    notificationClient));
                }
                if (prevMonitoring != null && prevMonitoring.getRaspberryPi() != null) {
                    log.info("Notifying Raspberry Pi '{}' [{}:{}] of sensor removal...",
                            prevMonitoring.getRaspberryPi().getName(),
                            prevMonitoring.getRaspberryPi().getIp(),
                            prevMonitoring.getRaspberryPi().getPort());
                    eventPublisher.publishEvent(
                            new NotifyRaspberryCommand(
                                    new StateChangeNotificationDTO(UpdateType.SENSOR_DELETE, LocalDateTime.now()),
                                    id,
                                    sensor.getWriteId(),
                                    prevMonitoring.getRaspberryPi(),
                                    notificationClient));
                }
            }
            if (connectionStatusChanged.get()) {
                liveDataService.pushConnectionStatusArduino(sensor.getReadId(), sensor.getStatus());
            }
            log.info("Sensor '{}' is updated in the system.", sensor.getName());
            return saved;
        }).orElseThrow(() -> new NotFoundException("Sensor station with id " + id + " was not found."));
    }

    /**
     * Returns the sensor station with the given ID.
     *
     * @param id the sensor station UUID
     * @return the matching {@link SensorStation}
     * @throws NotFoundException if no sensor station with that ID exists
     */
    @Override
    public SensorStation getSpecificSensor(UUID id) {
        return sensorRepository.findById(id).orElseThrow(() -> new NotFoundException("Sensor with id " + id + " was not found."));
    }

    /**
     * Disconnects the sensor station from its current room. If the room had a
     * Raspberry Pi assigned, it is notified via a {@link UpdateType#SENSOR_DELETE}
     * command.
     *
     * @param id the UUID of the sensor station to disconnect
     * @return the updated {@link SensorStation} with no room assignment
     * @throws NotFoundException if no sensor station with that ID exists
     */
    @Override
    @Transactional
    public SensorStation disconnectFromRoom(UUID id) {
        SensorStation station = sensorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sensor with id " + id + " was not found."));
        RoomMonitoring prevMonitoring = station.getRoomMonitoring();
        station.setRoomMonitoring(null);
        SensorStation saved = sensorRepository.save(station);
        if (prevMonitoring != null && prevMonitoring.getRaspberryPi() != null) {
            log.info("Sensor '{}' is disconnected from room '{}'.", saved.getName(), prevMonitoring.getRoomNumber());
            log.info("Notifying Raspberry Pi '{}' [{}:{}].", prevMonitoring.getRaspberryPi().getName(),
                    prevMonitoring.getRaspberryPi().getIp(), prevMonitoring.getRaspberryPi().getPort());
            eventPublisher.publishEvent(
                    new NotifyRaspberryCommand(
                            new StateChangeNotificationDTO(UpdateType.SENSOR_DELETE, LocalDateTime.now()),
                            id,
                            station.getWriteId(),
                            prevMonitoring.getRaspberryPi(),
                            notificationClient));
        }
        return saved;
    }

    /**
     * Deletes the sensor station with the given ID. If the station was assigned to a
     * room with a Raspberry Pi, the Pi is notified via a {@link UpdateType#SENSOR_DELETE}
     * command before the record is removed.
     *
     * @param id the UUID of the sensor station to delete
     * @throws NotFoundException if no sensor station with that ID exists
     */
    @Override
    @Transactional
    public void deleteById(UUID id) {
        SensorStation station = sensorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sensor with id " + id + " was not found."));
        RoomMonitoring monitoring = station.getRoomMonitoring();
        sensorRepository.deleteById(id);
        log.info("Sensor '{}' is deleted from the system.", station.getName());
        if (monitoring != null && monitoring.getRaspberryPi() != null) {
            log.info("Notifying Raspberry Pi '{}' [{}:{}] about deleted sensor.", monitoring.getRaspberryPi().getName(),
                    monitoring.getRaspberryPi().getIp(), monitoring.getRaspberryPi().getPort());
            eventPublisher.publishEvent(
                    new NotifyRaspberryCommand(
                            new StateChangeNotificationDTO(UpdateType.SENSOR_DELETE, LocalDateTime.now()),
                            id,
                            station.getWriteId(),
                            monitoring.getRaspberryPi(),
                            notificationClient));
        }
    }

    /**
     * Publishes a {@link NotifyRaspberryCommand} to retry the BLE connection between
     * the sensor station's Raspberry Pi and the station. Does nothing if the station
     * has no assigned room or the room has no Raspberry Pi.
     *
     * @param id the UUID of the sensor station
     * @throws NotFoundException if no sensor station with that ID exists
     */
    @Override
    public void retryConnection(UUID id) {
        SensorStation station = sensorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sensor station with id " + id + " was not found."));
        if (station.getRoomMonitoring() == null || station.getRoomMonitoring().getRaspberryPi() == null) return;
        RaspberryPi pi = station.getRoomMonitoring().getRaspberryPi();
        log.info("Retrying connection between Raspberry Pi '{}' [{}:{}] and sensor station '{}'", pi.getName(), pi.getIp(), pi.getPort(), station.getName());
        eventPublisher.publishEvent(new NotifyRaspberryCommand(
                station.getReadId(),
                station.getWriteId(),
                pi,
                notificationClient
        ));
    }
}