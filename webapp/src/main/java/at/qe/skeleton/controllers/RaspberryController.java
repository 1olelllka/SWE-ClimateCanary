package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.RaspberryCreateMapper;
import at.qe.skeleton.mappers.RaspberryMapper;
import at.qe.skeleton.mappers.RaspberryPatchMapper;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomOccupancy;
import at.qe.skeleton.services.RaspberryService;
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
@RequestMapping("/api/raspberry-pis")
@Tag(name = "Raspberry Pi Management")
public class RaspberryController {

    private final RaspberryService raspberryService;
    private final RaspberryMapper raspberryMapper;
    private final RaspberryCreateMapper raspberryCreateMapper;
    private final RaspberryPatchMapper raspberryPatchMapper;

    @Autowired
    public RaspberryController(RaspberryService raspberryService,
                               RaspberryMapper raspberryMapper,
                               RaspberryCreateMapper createMapper,
                               RaspberryPatchMapper raspberryPatchMapper) {
        this.raspberryService = raspberryService;
        this.raspberryMapper = raspberryMapper;
        this.raspberryCreateMapper = createMapper;
        this.raspberryPatchMapper = raspberryPatchMapper;
    }

    @GetMapping("")
    @Operation(summary = "Get page of Raspberries. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of raspberries."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    public ResponseEntity<Page<RaspberryDTO>> getAllRaspberries(Pageable pageable) {
        Page<RaspberryPi> pis = raspberryService.getAllRaspberries(pageable);
        return new ResponseEntity<>(pis.map(raspberryMapper::mapTo), HttpStatus.OK);
    }

    @Operation(summary = "Get specific Raspberry Pi. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Specific Raspberry."),
            @ApiResponse(responseCode = "404", description = "Raspberry Pi not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping("/{raspberry_id}")
    public ResponseEntity<RaspberryDTO> getSpecificRaspberry(@PathVariable(name="raspberry_id")UUID id) {
        RaspberryPi pi = raspberryService.getSpecificRaspberry(id);
        return new ResponseEntity<>(raspberryMapper.mapTo(pi), HttpStatus.OK);
    }

    @Operation(summary = "Synchronize occupancy for Raspberry Pi. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Specific Raspberry."),
            @ApiResponse(responseCode = "404", description = "Raspberry Pi not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping("/{raspberry_id}/sync/occupancy")
    public ResponseEntity<OccupancyDTO> syncOccupancy(@PathVariable(name="raspberry_id") UUID id) {
        RoomOccupancy occupancy = raspberryService.getOccupancyFromRedis(id);
        if (occupancy != null) {
            return new ResponseEntity<>(new OccupancyDTO(occupancy.getPeopleCnt(), occupancy.getRoomId(), occupancy.getPeopleCnt() < 5), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Send retry command to raspberry. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sent request."),
            @ApiResponse(responseCode = "404", description = "Raspberry not found.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping("/{raspberry_id}/retry-connection")
    public ResponseEntity<Void> retryDevicesConnection(@PathVariable(name="raspberry_id") UUID id) {
        raspberryService.retryConnection(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Create new Raspberry Pi. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created raspberry."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Name/Ip/Port conflicts.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping("")
    public ResponseEntity<RaspberryDTO> createNewRaspberry(@RequestBody @Valid RaspberryCreateDTO dto,
                                                           BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        RaspberryPi createdPi = raspberryService.createNewRaspberry(raspberryCreateMapper.mapFrom(dto));
        createdPi = raspberryService.addNewRoom(createdPi.getId(), dto.roomId());
        return new ResponseEntity<>(raspberryMapper.mapTo(createdPi), HttpStatus.CREATED);
    }

    @Operation(summary = "Patch Raspberry Pi. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patched Raspberry Pi."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Raspberry Pi not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Name/Ip/Port conflict.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PatchMapping("/{raspberry_id}")
    public ResponseEntity<RaspberryDTO> patchSpecificRaspberry(@PathVariable(name = "raspberry_id") UUID id,
                                                               @RequestBody @Valid RaspberryPatchDTO dto,
                                                               BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        RaspberryPi patched = raspberryService.updateRaspberryById(id, raspberryPatchMapper.mapFrom(dto));
        return new ResponseEntity<>(raspberryMapper.mapTo(patched), HttpStatus.OK);
    }

    @Operation(summary = "Add room to Raspberry Pi. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added new Room."),
            @ApiResponse(responseCode = "404", description = "Raspberry Pi/Room not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Room already assigned to other Raspberry..",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping("/{raspberry_id}/rooms/{room_id}")
    public ResponseEntity<RaspberryDTO> addTheRoomToRaspberry(@PathVariable(name="raspberry_id") UUID raspberry_id,
                                                      @PathVariable(name="room_id") UUID room_id) {
        RaspberryPi pi = raspberryService.addNewRoom(raspberry_id, room_id);
        return new ResponseEntity<>(raspberryMapper.mapTo(pi), HttpStatus.OK);
    }

    @Operation(summary = "Remove Room from Raspberry Pi. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully removed Room."),
            @ApiResponse(responseCode = "404", description = "Raspberry Pi/Room not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping("/{raspberry_id}/rooms/{room_id}")
    public ResponseEntity<RaspberryDTO> removeTheRoomFromRaspberry(@PathVariable(name="raspberry_id") UUID raspberry_id,
                                                              @PathVariable(name="room_id") UUID room_id) {
        RaspberryPi pi = raspberryService.removeRoomFromRaspberry(raspberry_id, room_id);
        return new ResponseEntity<>(raspberryMapper.mapTo(pi), HttpStatus.OK);
    }

    @Operation(summary = "Delete Raspberry Pi. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted Raspberry Pi."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping("/{raspberry_id}")
    public ResponseEntity<Void> deleteSpecificRaspberry(@PathVariable(name="raspberry_id") UUID id) {
        raspberryService.deleteRaspberry(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Get relevant Raspberry Pi Config. One of Permissions Required: CAN_MANAGE_DEVICES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Config."),
            @ApiResponse(responseCode = "404", description = "Raspberry Pi not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping("/{raspberry_id}/config")
    public ResponseEntity<PiConfigDTO> getRaspberryPiConfig(@PathVariable(name="raspberry_id") UUID id) {
        PiConfigDTO config = raspberryService.getConfigForRaspberry(id);
        return new ResponseEntity<>(config, HttpStatus.OK);
    }
}
