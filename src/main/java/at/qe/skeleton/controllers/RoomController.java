package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.LimitDTO;
import at.qe.skeleton.dtos.RoomCreateDTO;
import at.qe.skeleton.dtos.RoomDTO;
import at.qe.skeleton.dtos.RoomPatchDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.LimitMapper;
import at.qe.skeleton.mappers.RoomMapper;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.Userx;
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
@RequestMapping("/api/rooms")
public class RoomController {

    private RoomService roomService;
    private RoomMapper roomMapper;
    private LimitMapper limitMapper;

    public RoomController(RoomService roomService,
                          RoomMapper roomMapper,
                          LimitMapper mapper) {
        this.roomService = roomService;
        this.roomMapper = roomMapper;
        this.limitMapper = mapper;
    }

    @GetMapping("")
    public ResponseEntity<Page<RoomDTO>> getPageOfRooms(Pageable pageable) {
        Page<Room> rooms = roomService.getPageOfRooms(pageable);
        return new ResponseEntity<>(rooms.map(roomMapper::mapTo), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<RoomDTO> createNewRoom(@RequestBody @Valid RoomCreateDTO dto,
                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Room created = roomService.createRoom(Room
                .builder()
                        .roomType(dto.roomType())
                        .department(Department.builder().id(dto.departmentID()).build())
                        .defaultPeopleCnt(dto.defaultPeopleCount())
                        .isActive(dto.isActive())
                        .roomNumber(dto.name())
                .build());
        return new ResponseEntity<>(roomMapper.mapTo(created), HttpStatus.CREATED);
    }

    @PatchMapping("{room_id}")
    public ResponseEntity<RoomDTO> patchSpecificRoom(@PathVariable(name="room_id") UUID id,
                                                     @RequestBody @Valid RoomPatchDTO dto,
                                                     BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Room patched = roomService.patchRoom(id, Room
                .builder()
                .roomType(dto.roomType())
                .department(dto.departmentID() != null ? Department.builder().id(dto.departmentID()).build() : null)
                .defaultPeopleCnt(dto.defaultPeopleCount())
                .isActive(dto.isActive())
                .users(dto.users() != null ? dto.users().stream().map(u -> Userx.builder().id(u).build()).collect(Collectors.toSet()) : null)
                .roomNumber(dto.roomNumber())
                .build());
        return new ResponseEntity<>(roomMapper.mapTo(patched), HttpStatus.OK);
    }

    @DeleteMapping("{room_id}")
    public ResponseEntity<Void> deleteSpecificRoom(@PathVariable(name = "room_id") UUID id) {
        roomService.deleteRoom(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{room_id}/limits")
    public ResponseEntity<LimitDTO> getAllLimitsForTheRoom(@PathVariable(name = "room_id") UUID roomId) {
        RoomMonitoring monitoring = roomService.getRoomMonitoring(roomId);
        return new ResponseEntity<>(limitMapper.mapTo(monitoring), HttpStatus.OK);
    }

    @PatchMapping("/{room_id}/limits")
    public ResponseEntity<LimitDTO> updateLimitsForTheRoom(@PathVariable(name = "room_id") UUID roomId,
                                                           @RequestBody LimitDTO dto) {
        RoomMonitoring monitoring = roomService.updateLimits(roomId, dto);
        return new ResponseEntity<>(limitMapper.mapTo(monitoring), HttpStatus.OK);
    }

}
