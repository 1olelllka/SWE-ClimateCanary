package at.qe.skeleton.services.impl;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import at.qe.skeleton.dtos.UpdateType;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.SensorStationRepository;
import at.qe.skeleton.services.SensorStationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log
public class SensorStationServiceImpl implements SensorStationService {

    private final SensorStationRepository sensorRepository;
    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher eventPublisher;

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
        RoomMonitoring monitoring = station.getRoomMonitoring();
        if (monitoring != null && monitoring.getRaspberryPi() != null) {
            log.info("Notifying Raspberry Pi with new sensor being added...");
            eventPublisher.publishEvent(
                    new NotifyRaspberryCommand(
                            new StateChangeNotificationDTO(UpdateType.SENSOR_ADD, LocalDateTime.now()),
                            station.getReadId(),
                            station.getWriteId(),
                            monitoring.getRaspberryPi(),
                            notificationClient));
        }
        log.info("Added sensor into the system.");
        return station;
    }

    @Override
    @Transactional
    public SensorStation updateExistingSensor(UUID id, SensorStation sensorStation) {
        return sensorRepository.findById(id).map(sensor -> {
            boolean notifyRasp = false;
            if (sensorStation.getRoomMonitoring() != null && !sensorStation.getRoomMonitoring().equals(sensor.getRoomMonitoring())) {
                sensor.setRoomMonitoring(sensorStation.getRoomMonitoring());
                log.info("Changing the room of sensor...");
                notifyRasp = true;
            }
            Optional.ofNullable(sensorStation.getName()).ifPresent(name -> {
                if (!name.equals(sensor.getName())) {
                    if (sensorRepository.existsByName(name))
                        throw new ConflictException("Sensor station with name " + sensorStation.getName() + " already exists.");
                    sensor.setName(name);
                }
            });
            Optional.ofNullable(sensorStation.getStatus()).ifPresent(sensor::setStatus);
            Optional.ofNullable(sensorStation.getLastHeartBeat()).ifPresent(sensor::setLastHeartBeat);
            RoomMonitoring prevMonitoring = sensor.getRoomMonitoring();
            SensorStation saved = sensorRepository.save(sensor);
            if (notifyRasp) {
                if (saved.getRoomMonitoring().getRaspberryPi() != null) {
                    log.info("Notifying new Raspberry Pi of new sensor...");
                    eventPublisher.publishEvent(
                            new NotifyRaspberryCommand(
                                    new StateChangeNotificationDTO(UpdateType.SENSOR_ADD, LocalDateTime.now()),
                                    id,
                                    sensor.getWriteId(),
                                    saved.getRoomMonitoring().getRaspberryPi(),
                                    notificationClient));
                }
                if (prevMonitoring != null && prevMonitoring.getRaspberryPi() != null) {
                    log.info("Notifying old Raspberry Pi of sensor removal...");
                    eventPublisher.publishEvent(
                            new NotifyRaspberryCommand(
                                    new StateChangeNotificationDTO(UpdateType.SENSOR_DELETE, LocalDateTime.now()),
                                    id,
                                    sensor.getWriteId(),
                                    prevMonitoring.getRaspberryPi(),
                                    notificationClient));
                }
            }
            log.info("Sensor updated.");
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
        log.info("Sensor is deleted.");
        if (monitoring != null && monitoring.getRaspberryPi() != null) {
            log.info("Notifying Raspberry of deleted sensor...");
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
        eventPublisher.publishEvent(new NotifyRaspberryCommand(
                station.getReadId(),
                station.getWriteId(),
                pi,
                notificationClient
        ));
    }
}
