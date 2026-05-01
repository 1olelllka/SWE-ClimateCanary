package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.*;
import at.qe.skeleton.model.Absence;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.AbsenceService;
import at.qe.skeleton.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.repositories.RoomRepository;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Userx endpoints exposed by the server.
 *
 * This class is part of the skeleton project provided for students of the
* course "Software Engineering" offered by Innsbruck University.
 */
@RestController
@RequestMapping("/api/users")
public class UserxController {
 
    private final UserxMapper userMapper;
    private final UserxCreateMapper userxCreateMapper;
    private final UserPatchMapper userPatchMapper;
    private final UserService userService;
    private final AbsenceService absenceService;
    private final AbsenceListMapper absenceListMapper;
    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    @Autowired
    public UserxController(UserxMapper userMapper,
                           UserService userService,
                           UserxCreateMapper userxCreateMapper,
                           UserPatchMapper userPatchMapper,
                           AbsenceService absenceService,
                           AbsenceListMapper absenceListMapper,
                           RoomRepository roomRepository,
                           RoomMapper roomMapper) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.userxCreateMapper = userxCreateMapper;
        this.userPatchMapper = userPatchMapper;
        this.absenceService = absenceService;
        this.absenceListMapper = absenceListMapper;
        this.roomRepository = roomRepository;
        this.roomMapper = roomMapper;
    }

    @GetMapping("")
    public ResponseEntity<Page<UserxDTO>> getPageOfUsers(Pageable pageable) {
        Page<Userx> page = userService.getPageOfUsers(pageable);
        return new ResponseEntity<>(page.map(userMapper::mapTo), HttpStatus.OK);
    }

    @GetMapping("/{user_id}")
    public ResponseEntity<UserxDTO> getSpecificUser(@PathVariable(name = "user_id") UUID id) {
        Userx user = userService.getSpecificUser(id);
        return new ResponseEntity<>(userMapper.mapTo(user), HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<UserxDTO> getAuthenticatedUser(Authentication authentication) {
        Userx user = userService.getByUsername(authentication.getName());
        return new ResponseEntity<>(userMapper.mapTo(user), HttpStatus.OK);
    }

    @GetMapping("/me/absences")
    public ResponseEntity<Page<AbsenceListDTO>> getPageOfAbsencesOfAuthenticatedUser(Authentication authentication,
                                                                                     Pageable pageable) {
        Userx authenticated = userService.getByUsername(authentication.getName());
        Page<Absence> absences = absenceService.getAllAbsencesById(authenticated.getId(), pageable);
        return new ResponseEntity<>(absences.map(absenceListMapper::mapTo), HttpStatus.OK);
    }

    // TODO: WTF?
    @GetMapping("/me/department/rooms")
    @PreAuthorize("hasAuthority('CAN_VIEW_OWN_SHARED_CLIMATE')")
    public ResponseEntity<List<RoomDTO>> getRoomsOfAuthenticatedUsersDepartment(Authentication authentication) {
        Userx authenticated = userService.getByUsername(authentication.getName());

        if (authenticated.getMyRoom() == null || authenticated.getMyRoom().getDepartment() == null) {
            throw new ValidationException("User has no assigned room or department.");
        }

        UUID departmentId = authenticated.getMyRoom().getDepartment().getId();

        List<RoomDTO> rooms = roomRepository.findAll().stream()
                .filter(room -> room.getDepartment() != null)
                .filter(room -> room.getDepartment().getId().equals(departmentId))
                .map(roomMapper::mapTo)
                .toList();

        return new ResponseEntity<>(rooms, HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<UserxDTO> createNewUser(@RequestBody @Valid UserxCreateDTO dto,
                                                  BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Userx user = userService.createNewUser(userxCreateMapper.mapFrom(dto));
        return new ResponseEntity<>(userMapper.mapTo(user), HttpStatus.CREATED);
    }

    @PatchMapping("/{user_id}")
    public ResponseEntity<UserxDTO> patchSpecificUser(@PathVariable(name = "user_id") UUID id,
                                                      @RequestBody UserxPatchDTO dto) {
        Userx user = userService.updateUser(id, userPatchMapper.mapFrom(dto));
        return new ResponseEntity<>(userMapper.mapTo(user), HttpStatus.OK);
    }

    @DeleteMapping("/{user_id}")
    public ResponseEntity<Void> deleteSpecificUser(@PathVariable(name="user_id") UUID id) {
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
