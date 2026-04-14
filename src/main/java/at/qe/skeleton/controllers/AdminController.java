package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.UserxCreateDTO;
import at.qe.skeleton.dtos.UserxDTO;
import at.qe.skeleton.mappers.UserxCreateMapper;
import at.qe.skeleton.mappers.UserxMapper;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.UserxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controllers for admin users.
 *
 * This class is part of the skeleton project provided for students of the
* course "Software Engineering" offered by Innsbruck University.
 */
@Tag(name = "Admin Controller")
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

  private final UserxCreateMapper userCreateMapper;
  private final UserxMapper userMapper;
  private final UserxService userService;

  @Autowired
  public AdminController(UserxCreateMapper userCreateMapper, UserxMapper userMapper,
      UserxService userService) {
    this.userCreateMapper = userCreateMapper;
    this.userMapper = userMapper;
    this.userService = userService;
  }

  /**
   * GET all existing Users
   *
   * @return {@link ResponseEntity} with status {@code 200 (OK)} with a collection of all existing
   * users in the body
   */
  @Operation(summary = "Get all Users ", description = "Get all existing Users.")
  @ApiResponse(responseCode = "200", description = "All Users returned.")
  @ApiResponse(responseCode = "404", description = "No Users found.")
  @GetMapping("")
  public ResponseEntity<Collection<UserxDTO>> getAllUsers() {
    Collection<Userx> allUsers = userService.getAllUsers();
    List<UserxDTO> allUsersMapped = allUsers.stream().map(user -> userMapper.mapTo(user)).toList();
    return ResponseEntity.ok(allUsersMapped);
  }

  /**
   * GET one User
   *
   * @param id the id to search for
   * @return {@link ResponseEntity} with status {@code 200 (OK)} with the user of given id in the
   * body, or with status {@code 404} if no such user exists
   */
  @Operation(summary = "Get a User", description = "Get a User by id.")
  @ApiResponse(responseCode = "200", description = "The User found.")
  @ApiResponse(responseCode = "404", description = "No User found.")
  @GetMapping("/{id}")
  public ResponseEntity<UserxDTO> getUser(
      @Parameter(description = "The id of the User to get.") @PathVariable UUID id) {
    Optional<Userx> existingUserx = userService.loadUser(id);
    if (existingUserx.isPresent()) {
      return ResponseEntity.ok(userMapper.mapTo(existingUserx.get()));
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Creates a user if the username is not yet used.
   *
   * @param userxDto the user tb created
   * @return {@link ResponseEntity} with status {@code 201 (Created)} with the newly created user in
   * the body, or with status {@code 409 (Conflict)} if the username is already in use
   */
  @Operation(summary = "Create User", description = "Create a new User.")
  @ApiResponse(responseCode = "201", description = "User created.")
  @ApiResponse(responseCode = "409", description = "User already exists.")
  @PostMapping("")
  public ResponseEntity<UserxDTO> createUser(@Valid @RequestBody UserxCreateDTO userxDto) {
    Userx user = userService.saveUser(userCreateMapper.mapFrom(userxDto));
    return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.mapTo(user));
  }

  /**
   * Partially updates user of given id.
   *
   * The update is partial because only a select subset of user fields can be modified after
   * create.
   *
   * @param id       the id of the user tb updated
   * @param userxDto the updated user information
   * @return {@link ResponseEntity} with status {@code 201 (Created)} with the updated user in the
   * body, or with status {@code 404 (Not Found)} if no user with this id exists
   */
  @Operation(summary = "Update User", description = "Update a User by id.")
  @ApiResponse(responseCode = "200", description = "The updated User.")
  @ApiResponse(responseCode = "404", description = "User not found.")
  @PatchMapping("/{id}")
  public ResponseEntity<UserxDTO> updateUser(
      @Parameter(name = "id", description = "The id of the User to update.") @PathVariable UUID id,
      @Parameter(name = "userxDto", description = "The User to update.") @Valid @RequestBody
      UserxDTO userxDto) {
    Optional<Userx> existingUserx = userService.loadUser(id);
    if (existingUserx.isPresent()) {
      Userx user = userMapper.mapFrom(userxDto);
      Userx savedUser = userService.saveUser(user);
      return ResponseEntity.ok(userMapper.mapTo(savedUser));
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Partially updates user of given id.
   *
   * The update is partial because only a select subset of user fields can be modified after
   * create.
   *
   * @param id the id of the user tb updated
   * @return {@link ResponseEntity} with status {@code 204 (No Content)} on successful delete, or
   * with status {@code 404 (Not Found)} if no user with this id exists
   */
  @Operation(summary = "Delete User", description = "Delete a User by id.")
  @ApiResponse(responseCode = "204", description = "User deleted.")
  @ApiResponse(responseCode = "404", description = "User not found.")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(
      @Parameter(name = "id", description = "The id of the User to delete.") @PathVariable
      UUID id) {
    Optional<Userx> existingUserx = userService.loadUser(id);
    if (existingUserx.isPresent()) {
      userService.deleteUser(existingUserx.get());
      return ResponseEntity.noContent().build();
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }
  }
}
