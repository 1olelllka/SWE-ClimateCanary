package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.SensorStationCreateDTO;
import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.SensorStationCreateMapper;
import at.qe.skeleton.mappers.SensorStationMapper;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.services.SensorStationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sensor-stations")
public class SensorStationController {

    private SensorStationService sensorStationService;
    private SensorStationMapper sensorStationMapper;
    private SensorStationCreateMapper sensorStationCreateMapper;

    @Autowired
    public SensorStationController(SensorStationService sensorStationService,
                                   SensorStationMapper sensorStationMapper,
                                   SensorStationCreateMapper sensorStationCreateMapper) {
        this.sensorStationService = sensorStationService;
        this.sensorStationMapper = sensorStationMapper;
        this.sensorStationCreateMapper = sensorStationCreateMapper;
    }


    @GetMapping("")
    public ResponseEntity<Page<SensorStationDTO>> getAllSensorStations(Pageable pageable) {
        Page<SensorStation> sensors = sensorStationService.getAllSensorStations(pageable);
        return new ResponseEntity<>(sensors.map(sensorStationMapper::mapTo), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<SensorStationDTO> createNewSensorStation(@RequestBody @Valid SensorStationCreateDTO dto,
                                                                   BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        SensorStation created = sensorStationService.createNewSensorStation(sensorStationCreateMapper.mapFrom(dto));
        return new ResponseEntity<>(sensorStationMapper.mapTo(created), HttpStatus.CREATED);
    }

    @PatchMapping("/{sensor_id}")
    public ResponseEntity<SensorStationDTO> patchExistingSensorStation(@PathVariable(name = "sensor_id") UUID id,
                                                                       @RequestBody @Valid SensorStationCreateDTO dto,
                                                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        SensorStation sensorStation = sensorStationService.updateExistingSensor(id, sensorStationCreateMapper.mapFrom(dto));
        return new ResponseEntity<>(sensorStationMapper.mapTo(sensorStation), HttpStatus.OK);
    }

    @GetMapping("/{sensor_id}")
    public ResponseEntity<SensorStationDTO> getSpecificSensorById(@PathVariable(name = "sensor_id") UUID id) {
        SensorStation station = sensorStationService.getSpecificSensor(id);
        return new ResponseEntity<>(sensorStationMapper.mapTo(station), HttpStatus.OK);
    }

    @DeleteMapping("/{sensor_id}")
    public ResponseEntity<Void> removeSpecificSensor(@PathVariable(name = "sensor_id") UUID id) {
        sensorStationService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
