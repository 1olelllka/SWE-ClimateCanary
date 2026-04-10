package at.qe.skeleton.services.impl;

import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
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
    private final RoomMonitoringRepository monitoringRepository;

    @Autowired
    public SensorStationServiceImpl(SensorStationRepository sensorRepository,
                                    NotificationClient notificationClient,
                                    RoomMonitoringRepository monitoringRepository) {
        this.sensorRepository = sensorRepository;
        this.notificationClient = notificationClient;
        this.monitoringRepository = monitoringRepository;
    }

    @Override
    public Page<SensorStation> getAllSensorStations(Pageable pageable) {
        return sensorRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public SensorStation createNewSensorStation(SensorStation sensorStation, UUID roomId) {
        RoomMonitoring desiredRoom = monitoringRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
        if (sensorRepository.existsByName(sensorStation.getName())) {
            throw new ConflictException("Sensor station with name " + sensorStation.getName() + " already exists.");
        }
        SensorStation station = sensorRepository.save(sensorStation);
        desiredRoom.setSensorStation(station);
        monitoringRepository.save(desiredRoom);
        // notify raspberry
        /*
        StateChangeNotificationDTO raspDto = new StateChangeNotificationDTO(UpdateType.SENSORS, LocalDateTime.now());
        notificationClient.notifyRaspberryAboutChanges(raspDto);
         */
        return station;
    }

    @Override
    @Transactional
    public SensorStation updateExistingSensor(UUID id, SensorStation sensorStation, UUID roomId) {
        return sensorRepository.findById(id).map(sensor -> {
                if (sensor.getRoomMonitoring() == null || roomId != null && !roomId.equals(sensor.getRoomMonitoring().getRoomId())){
                    RoomMonitoring monitoring = monitoringRepository.findById(roomId)
                            .orElseThrow(() -> new NotFoundException("Room with id " + roomId + " was not found."));
                    sensor.setRoomMonitoring(monitoring);
                    monitoring.setSensorStation(sensor);
                    monitoringRepository.save(monitoring);
                }
                Optional.ofNullable(sensorStation.getName()).ifPresent(name -> {
                    if (!name.equals(sensor.getName())) {
                        if (sensorRepository.existsByName(name)) throw new ConflictException("Sensor station with name " + sensorStation.getName() + " already exists.");
                        sensor.setName(name);
                    }
                });
                Optional.ofNullable(sensorStation.getStatus()).ifPresent(sensor::setStatus);
                Optional.ofNullable(sensorStation.getLastHeartBeat()).ifPresent(sensor::setLastHeartBeat);
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
        SensorStation station = sensorRepository.findById(id).orElseThrow(() -> new NotFoundException("Sensor with id " + id + " was not found."));
        if (station.getRoomMonitoring() != null) {
            station.getRoomMonitoring().setSensorStation(null);
            monitoringRepository.save(station.getRoomMonitoring());
        }
        sensorRepository.deleteById(id);
    }

}