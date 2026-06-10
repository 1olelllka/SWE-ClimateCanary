package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.SensorStationCreateDTO;
import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.dtos.SensorStationPatchDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.SensorStationCreateMapper;
import at.qe.skeleton.mappers.SensorStationMapper;
import at.qe.skeleton.mappers.SensorStationPatchMapper;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.services.SensorStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sensor-stations")
@Tag(name = "Sensor Station Management")
public class SensorStationController {

    private SensorStationService sensorStationService;
    private SensorStationMapper sensorStationMapper;
    private SensorStationCreateMapper sensorStationCreateMapper;
    private SensorStationPatchMapper sensorStationPatchMapper;

    @Autowired
    public SensorStationController(SensorStationService sensorStationService,
                                   SensorStationMapper sensorStationMapper,
                                   SensorStationCreateMapper sensorStationCreateMapper,
                                   SensorStationPatchMapper sensorStationpatchMapper) {
        this.sensorStationService = sensorStationService;
        this.sensorStationMapper = sensorStationMapper;
        this.sensorStationCreateMapper = sensorStationCreateMapper;
        this.sensorStationPatchMapper = sensorStationpatchMapper;
    }


    @Operation(summary = "Get page of sensors. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of sensors."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping("")
    public ResponseEntity<Page<SensorStationDTO>> getAllSensorStations(Pageable pageable) {
        Page<SensorStation> sensors = sensorStationService.getAllSensorStations(pageable);
        return new ResponseEntity<>(sensors.map(sensorStationMapper::mapTo), HttpStatus.OK);
    }

    @Operation(summary = "Create new sensor station. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created sensor."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Name conflict.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Unauthorized.")
    })
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

    @Operation(summary = "Patch sensor station. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patched sensor station."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Sensor station not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Naming issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PatchMapping("/{sensor_id}")
    public ResponseEntity<SensorStationDTO> patchExistingSensorStation(@PathVariable(name = "sensor_id") UUID id,
                                                                       @RequestBody @Valid SensorStationPatchDTO dto,
                                                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        SensorStation sensorStation = sensorStationService.updateExistingSensor(id, sensorStationPatchMapper.mapFrom(dto));
        return new ResponseEntity<>(sensorStationMapper.mapTo(sensorStation), HttpStatus.OK);
    }

    @Operation(summary = "Send retry command to raspberry. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sent request."),
            @ApiResponse(responseCode = "404", description = "Sensor station not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping("{sensor_id}/retry-connection")
    public ResponseEntity<Void> retrySensorStation(@PathVariable(name = "sensor_id") UUID id) {
        sensorStationService.retryConnection(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Get specific sensor. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Specific sensor station."),
            @ApiResponse(responseCode = "404", description = "Sensor not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping("/{sensor_id}")
    public ResponseEntity<SensorStationDTO> getSpecificSensorById(@PathVariable(name = "sensor_id") UUID id) {
        SensorStation station = sensorStationService.getSpecificSensor(id);
        return new ResponseEntity<>(sensorStationMapper.mapTo(station), HttpStatus.OK);
    }

    @Operation(summary = "Disconnect sensor from a room. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Specific sensor station."),
            @ApiResponse(responseCode = "404", description = "Sensor not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping("/{sensor_id}/room")
    public ResponseEntity<SensorStationDTO> disconnectSensorFromRoom(@PathVariable(name = "sensor_id") UUID id) {
        SensorStation station = sensorStationService.disconnectFromRoom(id);
        return new ResponseEntity<>(sensorStationMapper.mapTo(station), HttpStatus.OK);
    }

    @Operation(summary = "Delete sensor. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted sensor station."),
            @ApiResponse(responseCode = "404", description = "Sensor not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping("/{sensor_id}")
    public ResponseEntity<Void> removeSpecificSensor(@PathVariable(name = "sensor_id") UUID id) {
        sensorStationService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
