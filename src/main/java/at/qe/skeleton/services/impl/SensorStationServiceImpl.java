package at.qe.skeleton.services.impl;

import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import at.qe.skeleton.dtos.UpdateType;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
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
            eventPublisher.publishEvent(
                    new NotifyRaspberryCommand(
                            new StateChangeNotificationDTO(UpdateType.SENSORS, LocalDateTime.now()),
                            station.getId(),
                            monitoring.getRaspberryPi(),
                            notificationClient));
        }
        return station;
    }

    @Override
    @Transactional
    public SensorStation updateExistingSensor(UUID id, SensorStation sensorStation) {
        return sensorRepository.findById(id).map(sensor -> {
            if (sensorStation.getRoomMonitoring() != null && !sensorStation.getRoomMonitoring().equals(sensor.getRoomMonitoring())) {
                sensor.setRoomMonitoring(sensorStation.getRoomMonitoring());
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
            SensorStation saved = sensorRepository.save(sensor);
            if (saved.getRoomMonitoring() != null && saved.getRoomMonitoring().getRaspberryPi() != null) {
                eventPublisher.publishEvent(
                        new NotifyRaspberryCommand(
                                new StateChangeNotificationDTO(UpdateType.SENSORS, LocalDateTime.now()),
                                id,
                                saved.getRoomMonitoring().getRaspberryPi(),
                                notificationClient));
            }
            return saved;
        }).orElseThrow(() -> new NotFoundException("Sensor station with id " + id + " was not found."));
    }

    @Override
    public SensorStation getSpecificSensor(UUID id) {
        return sensorRepository.findById(id).orElseThrow(() -> new NotFoundException("Sensor with id " + id + " was not found."));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        SensorStation station = sensorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sensor with id " + id + " was not found."));
        RoomMonitoring monitoring = station.getRoomMonitoring();
        sensorRepository.deleteById(id);
        if (monitoring != null && monitoring.getRaspberryPi() != null) {
            eventPublisher.publishEvent(
                    new NotifyRaspberryCommand(
                            new StateChangeNotificationDTO(UpdateType.SENSORS, LocalDateTime.now()),
                            null,
                            monitoring.getRaspberryPi(),
                            notificationClient));
        }
    }
}
