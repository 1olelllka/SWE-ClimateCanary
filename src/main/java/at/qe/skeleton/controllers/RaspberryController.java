package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.RaspberryCreateMapper;
import at.qe.skeleton.mappers.RaspberryMapper;
import at.qe.skeleton.mappers.RaspberryPatchMapper;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomOccupancy;
import at.qe.skeleton.services.RaspberryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/raspberry-pis")
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
    public ResponseEntity<Page<RaspberryDTO>> getAllRaspberries(Pageable pageable) {
        Page<RaspberryPi> pis = raspberryService.getAllRaspberries(pageable);
        return new ResponseEntity<>(pis.map(raspberryMapper::mapTo), HttpStatus.OK);
    }

    @GetMapping("/{raspberry_id}")
    public ResponseEntity<RaspberryDTO> getSpecificRaspberry(@PathVariable(name="raspberry_id")UUID id) {
        RaspberryPi pi = raspberryService.getSpecificRaspberry(id);
        return new ResponseEntity<>(raspberryMapper.mapTo(pi), HttpStatus.OK);
    }

    @GetMapping("/{raspberry_id}/sync/occupancy")
    public ResponseEntity<List<OccupancyDTO>> syncOccupancy(@PathVariable(name="raspberry_id") UUID id) {
        List<RoomOccupancy> occupancy = raspberryService.getOccupancyFromRedis(id);
        return new ResponseEntity<>(occupancy.stream().map(r -> new OccupancyDTO(r.getPeopleCnt(), r.getRoomId(), r.getPeopleCnt() < 5)).toList(), HttpStatus.OK);
    }

    @PostMapping("/{raspberry_id}/retry-connection")
    public ResponseEntity<Void> retryDevicesConnection(@PathVariable(name="raspberry_id") UUID id) {
        raspberryService.retryConnection(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<RaspberryDTO> createNewRaspberry(@RequestBody @Valid RaspberryCreateDTO dto,
                                                           BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        RaspberryPi createdPi = raspberryService.createNewRaspberry(raspberryCreateMapper.mapFrom(dto));
        return new ResponseEntity<>(raspberryMapper.mapTo(createdPi), HttpStatus.CREATED);
    }

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

    @PostMapping("/{raspberry_id}/rooms/{room_id}")
    public ResponseEntity<RaspberryDTO> addTheRoomToRaspberry(@PathVariable(name="raspberry_id") UUID raspberry_id,
                                                      @PathVariable(name="room_id") UUID room_id) {
        RaspberryPi pi = raspberryService.addNewRoom(raspberry_id, room_id);
        return new ResponseEntity<>(raspberryMapper.mapTo(pi), HttpStatus.OK);
    }

    @DeleteMapping("/{raspberry_id}/rooms/{room_id}")
    public ResponseEntity<RaspberryDTO> removeTheRoomFromRaspberry(@PathVariable(name="raspberry_id") UUID raspberry_id,
                                                              @PathVariable(name="room_id") UUID room_id) {
        RaspberryPi pi = raspberryService.removeRoomFromRaspberry(raspberry_id, room_id);
        return new ResponseEntity<>(raspberryMapper.mapTo(pi), HttpStatus.OK);
    }

    @DeleteMapping("/{raspberry_id}")
    public ResponseEntity<Void> deleteSpecificRaspberry(@PathVariable(name="raspberry_id") UUID id) {
        raspberryService.deleteRaspberry(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{raspberry_id}/config")
    public ResponseEntity<PiConfigDTO> getRaspberryPiConfig(@PathVariable(name="raspberry_id") UUID id) {
        PiConfigDTO config = raspberryService.getConfigForRaspberry(id);
        return new ResponseEntity<>(config, HttpStatus.OK);
    }
}
