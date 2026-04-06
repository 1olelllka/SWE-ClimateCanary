package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.*;
import at.qe.skeleton.model.Absence;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.AbsenceService;
import at.qe.skeleton.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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
    private final UserListMapper userListMapper;
    private final UserService userService;
    private final AbsenceService absenceService;
    private final AbsenceListMapper absenceListMapper;

    @Autowired
    public UserxController(UserxMapper userMapper,
                           UserService userService,
                           UserxCreateMapper userxCreateMapper,
                           UserPatchMapper userPatchMapper,
                           UserListMapper userListMapper,
                           AbsenceService absenceService,
                           AbsenceListMapper absenceListMapper) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.userxCreateMapper = userxCreateMapper;
        this.userListMapper = userListMapper;
        this.userPatchMapper = userPatchMapper;
        this.absenceService = absenceService;
        this.absenceListMapper = absenceListMapper;
    }

    @GetMapping("")
    public ResponseEntity<Page<UserxListDTO>> getPageOfUsers(Pageable pageable) {
        Page<Userx> page = userService.getPageOfUsers(pageable);
        return new ResponseEntity<>(page.map(userListMapper::mapTo), HttpStatus.OK);
    }

    @GetMapping("/{user_id}")
    public ResponseEntity<UserxDTO> getSpecificUser(@PathVariable(name = "user_id") UUID id) {
        Userx user = userService.getSpecificUser(id);
        return new ResponseEntity<>(userMapper.mapTo(user), HttpStatus.OK);
    }

    @GetMapping("/me/absences")
    public ResponseEntity<Page<AbsenceListDTO>> getPageOfAbsencesOfAuthenticatedUser(Authentication authentication,
                                                                                     Pageable pageable) {
        // authentication.isAuthenticated() check is redundant here, as the endpoint requires specific permission
        // which cannot be achieved if user is anonymous
        Userx authenticated = (Userx) authentication.getCredentials();
        Page<Absence> absences = absenceService.getAllAbsencesById(authenticated.getId(), pageable);
        return new ResponseEntity<>(absences.map(absenceListMapper::mapTo), HttpStatus.OK);
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
