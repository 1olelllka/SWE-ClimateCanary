package at.qe.skeleton.services.impl;

import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.SensorStationRepository;
import at.qe.skeleton.services.SensorStationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class SensorStationServiceImpl implements SensorStationService {

    private final SensorStationRepository sensorRepository;
    private final NotificationClient notificationClient;

    @Autowired
    public SensorStationServiceImpl(SensorStationRepository sensorRepository,
                                    NotificationClient notificationClient) {
        this.sensorRepository = sensorRepository;
        this.notificationClient = notificationClient;
    }

    @Override
    public Page<SensorStation> getAllSensorStations(Pageable pageable) {
        return sensorRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public SensorStation createNewSensorStation(SensorStation sensorStation) {
        RoomMonitoring desiredRoom = sensorStation.getRoomMonitoring();
        if (desiredRoom.getSensorStation() != null) {
            throw new ConflictException("The room already has the sensor. Delete the existing sensor in this room before adding a new one.");
        }
        if (sensorRepository.existsByName(sensorStation.getName())) {
            throw new ConflictException("Sensor station with name " + sensorStation.getName() + " already exists.");
        }
        SensorStation station = sensorRepository.save(sensorStation);
        desiredRoom.setSensorStation(station);
        // notify raspberry
        /*
        StateChangeNotificationDTO raspDto = new StateChangeNotificationDTO(UpdateType.SENSORS, LocalDateTime.now());
        notificationClient.notifyRaspberryAboutChanges(raspDto);
         */
        return station;
    }

    @Override
    @Transactional
    public SensorStation updateExistingSensor(UUID id, SensorStation sensorStation) {
        return sensorRepository.findById(id).map(sensor -> {
                Optional.of(sensorStation.getRoomMonitoring()).ifPresent(r -> {
                    sensor.setRoomMonitoring(r);
                    sensor.getRoomMonitoring().setSensorStation(sensor);
                });
                Optional.of(sensorStation.getName()).ifPresent(sensor::setName);
                // notify raspberry...
                /*
                StateChangeNotificationDTO raspDto = new StateChangeNotificationDTO(UpdateType.SENSORS, LocalDateTime.now());
                notificationClient.notifyRaspberryAboutChanges(raspDto);
                */
                return sensorRepository.save(sensor);
        }).orElseThrow(() -> new NotFoundException("Sensor station with id " + id + " was not found."));
    }

    @Override
    public SensorStation getSpecificSensor(UUID id) {
        return sensorRepository.findById(id).orElseThrow(() -> new NotFoundException("Sensor with id " + id + " was not found."));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        if (sensorRepository.existsById(id)) {
            sensorRepository.deleteById(id);
            // notify raspberry...
            /*
            StateChangeNotificationDTO raspDto = new StateChangeNotificationDTO(UpdateType.SENSORS, LocalDateTime.now());
            notificationClient.notifyRaspberryAboutChanges(raspDto);
             */
            return;
        }
        throw new NotFoundException("Sensor with id " + id + " was not found.");
    }

}