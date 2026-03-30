package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.RoomDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.RoomMapping;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.services.RoomService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private RoomService roomService;
    private RoomMapping roomMapping;

    public RoomController(RoomService roomService,
                          RoomMapping roomMapping) {
        this.roomService = roomService;
        this.roomMapping = roomMapping;
    }

    @GetMapping("")
    public ResponseEntity<Page<RoomDTO>> getPageOfRooms(Pageable pageable) {
        Page<Room> rooms = roomService.getPageOfRooms(pageable);
        return new ResponseEntity<>(rooms.map(roomMapping::mapTo), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<RoomDTO> createNewRoom(@RequestBody @Valid RoomDTO dto,
                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Room created = roomService.createRoom(roomMapping.mapFrom(dto));
        return new ResponseEntity<>(roomMapping.mapTo(created), HttpStatus.CREATED);
    }

    @PatchMapping("{room_id}")
    public ResponseEntity<RoomDTO> patchSpecificRoom(@PathVariable(name="room_id") UUID id,
                                                     @RequestBody @Valid RoomDTO dto,
                                                     BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Room patched = roomService.patchRoom(id, roomMapping.mapFrom(dto));
        return new ResponseEntity<>(roomMapping.mapTo(patched), HttpStatus.OK);
    }

    @DeleteMapping("{room_id}")
    public ResponseEntity<Void> deleteSpecificRoom(@PathVariable(name = "room_id") UUID id) {
        roomService.deleteRoom(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
