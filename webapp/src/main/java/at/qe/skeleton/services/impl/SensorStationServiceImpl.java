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

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorStationServiceImpl implements SensorStationService {

    private final SensorStationRepository sensorRepository;
    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher eventPublisher;
    private final LiveDataService liveDataService;

    @Override
    public Page<SensorStation> getAllSensorStations(Pageable pageable) {
        return sensorRepository.findAll(pageable);
    }

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

    @Override
    public SensorStation getSpecificSensor(UUID id) {
        return sensorRepository.findById(id).orElseThrow(() -> new NotFoundException("Sensor with id " + id + " was not found."));
    }

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
