package at.qe.skeleton.services;

import at.qe.skeleton.model.SensorStation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SensorStationService {
    Page<SensorStation> getAllSensorStations(Pageable pageable);

    SensorStation createNewSensorStation(SensorStation sensorStation);

    SensorStation updateExistingSensor(UUID id, SensorStation sensorStation);

    SensorStation getSpecificSensor(UUID id);

    void deleteById(UUID id);

    void retryConnection(UUID id);
}
