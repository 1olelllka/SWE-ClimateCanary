package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.UserxCreateDTO;
import at.qe.skeleton.dtos.UserxDTO;
import at.qe.skeleton.dtos.UserxListDTO;
import at.qe.skeleton.dtos.UserxPatchDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.*;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
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

    @Autowired
    public UserxController(UserxMapper userMapper,
                           UserService userService,
                           UserxCreateMapper userxCreateMapper,
                           UserPatchMapper userPatchMapper,
                           UserListMapper userListMapper) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.userxCreateMapper = userxCreateMapper;
        this.userListMapper = userListMapper;
        this.userPatchMapper = userPatchMapper;
    }

    @GetMapping("")
    public ResponseEntity<Page<UserxListDTO>> getPageOfUsers(Pageable pageable) {
        Page<Userx> page = userService.getPageOfUsers(pageable);
        return new ResponseEntity<>(page.map(userListMapper::mapTo), HttpStatus.OK);
    }

    @GetMapping("{user_id}")
    public ResponseEntity<UserxDTO> getSpecificUser(@PathVariable(name = "user_id") UUID id) {
        Userx user = userService.getSpecificUser(id);
        return new ResponseEntity<>(userMapper.mapTo(user), HttpStatus.OK);
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

    @PatchMapping("{user_id}")
    public ResponseEntity<UserxDTO> patchSpecificUser(@PathVariable(name = "user_id") UUID id,
                                                      @RequestBody @Valid UserxPatchDTO dto,
                                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Userx user = userService.updateUser(id, userPatchMapper.mapFrom(dto));
        return new ResponseEntity<>(userMapper.mapTo(user), HttpStatus.OK);
    }

    @DeleteMapping("{user_id}")
    public ResponseEntity<Void> deleteSpecificUser(@PathVariable(name="user_id") UUID id) {
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
