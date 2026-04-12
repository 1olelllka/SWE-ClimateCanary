package at.qe.skeleton.services;

import at.qe.skeleton.model.SensorStation;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SensorStationService {
    Page<SensorStation> getAllSensorStations(Pageable pageable);

    SensorStation createNewSensorStation(SensorStation sensorStation, @NotNull @NotEmpty UUID roomId);

    SensorStation updateExistingSensor(UUID id, SensorStation sensorStation, @NotNull @NotEmpty UUID roomId);

    SensorStation getSpecificSensor(UUID id);

    void deleteById(UUID id);
}
