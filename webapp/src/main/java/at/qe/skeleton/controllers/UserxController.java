package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.mappers.AbsenceListMapper;
import at.qe.skeleton.mappers.UserPatchMapper;
import at.qe.skeleton.mappers.UserxCreateMapper;
import at.qe.skeleton.mappers.UserxMapper;
import at.qe.skeleton.model.Absence;
import at.qe.skeleton.model.UserSettings;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.AbsenceService;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.UserService;
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
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User management")
public class UserxController {
 
    private final UserxMapper userMapper;
    private final UserxCreateMapper userxCreateMapper;
    private final UserPatchMapper userPatchMapper;
    private final UserService userService;
    private final AbsenceService absenceService;
    private final AbsenceListMapper absenceListMapper;
    private final AuthenticatedUserService authenticatedUserService;

    @Autowired
    public UserxController(UserxMapper userMapper,
                           UserService userService,
                           UserxCreateMapper userxCreateMapper,
                           UserPatchMapper userPatchMapper,
                           AbsenceService absenceService,
                           AbsenceListMapper absenceListMapper,
                           AuthenticatedUserService authenticatedUserService) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.userxCreateMapper = userxCreateMapper;
        this.userPatchMapper = userPatchMapper;
        this.absenceService = absenceService;
        this.absenceListMapper = absenceListMapper;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Operation(summary = "Get Page of Users. One of Permissions Required: CAN_MANAGE_USERS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of users."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping("")
    public ResponseEntity<Page<UserxDTO>> getPageOfUsers(Pageable pageable) {
        Page<Userx> page = userService.getPageOfUsers(pageable);
        return new ResponseEntity<>(page.map(userMapper::mapTo), HttpStatus.OK);
    }

    @Operation(summary = "Get specific user. One of Permissions Required: CAN_MANAGE_USERS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User."),
            @ApiResponse(responseCode = "404", description = "User not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping("/{user_id}")
    public ResponseEntity<UserxDTO> getSpecificUser(@PathVariable(name = "user_id") UUID id) {
        Userx user = userService.getSpecificUser(id);
        return new ResponseEntity<>(userMapper.mapTo(user), HttpStatus.OK);
    }

    @Operation(summary = "Get authenticated User.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping("/me")
    public ResponseEntity<UserxDTO> getAuthenticatedUser(Authentication authentication) {
        Userx user = userService.getByUsername(authentication.getName());
        return new ResponseEntity<>(userMapper.mapTo(user), HttpStatus.OK);
    }

    @Operation(summary = "Get page of Absences. One of Permissions Required: CAN_MANAGE_OWN_ABSENCES")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of absences."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping("/me/absences")
    public ResponseEntity<Page<AbsenceListDTO>> getPageOfAbsencesOfAuthenticatedUser(Authentication authentication,
                                                                                     Pageable pageable) {
        Userx authenticated = userService.getByUsername(authentication.getName());
        Page<Absence> absences = absenceService.getAllAbsencesById(authenticated.getId(), pageable);
        return new ResponseEntity<>(absences.map(absenceListMapper::mapTo), HttpStatus.OK);
    }

    @Operation(summary = "Create new User. One of Permissions Required: CAN_MANAGE_USERS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created user."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Username conflict.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
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

    @Operation(summary = "Patch specific User. One of Permissions Required: CAN_MANAGE_USERS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patched user."),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Username conflict.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PatchMapping("/{user_id}")
    public ResponseEntity<UserxDTO> patchSpecificUser(@PathVariable(name = "user_id") UUID id,
                                                      @RequestBody UserxPatchDTO dto) {
        Userx user = userService.updateUser(id, userPatchMapper.mapFrom(dto));
        return new ResponseEntity<>(userMapper.mapTo(user), HttpStatus.OK);
    }

    @Operation(summary = "Patch specific User. One of Permissions Required: CAN_MANAGE_USERS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping("/{user_id}")
    public ResponseEntity<Void> deleteSpecificUser(@PathVariable(name="user_id") UUID id) {
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Get settings of a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User settings."),
            @ApiResponse(responseCode = "404", description = "User settings not found.",
            content = @Content(mediaType = "application/json",schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @GetMapping("/settings")
    public ResponseEntity<UserSettingsDTO> getUserSettings() {
        Userx user = authenticatedUserService.getAuthenticatedUser();
        UserSettings s = userService.getUserSettings(user.getId());
        return new ResponseEntity<>(toDTO(s), HttpStatus.OK);
    }

    @Operation(summary = "Patch settings of a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patched User settings."),
            @ApiResponse(responseCode = "404", description = "User settings not found.",
                    content = @Content(mediaType = "application/json",schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @PatchMapping("/settings")
    public ResponseEntity<UserSettingsDTO> updateUserSettings(@RequestBody UserSettingsPatchDTO dto) {
        Userx user = authenticatedUserService.getAuthenticatedUser();
        UserSettings s = userService.updateUserSettings(user.getId(), dto);
        return new ResponseEntity<>(toDTO(s), HttpStatus.OK);
    }

    private UserSettingsDTO toDTO(UserSettings s) {
        return new UserSettingsDTO(
                s.getUserId(), s.isDarkMode(), s.isFahrenheit(), s.getFormat(), s.isTwelveHourFormat(),
                s.getNotificationEmail(), s.isEmailWarnings(), s.isEmailAbsences()
        );
    }
}
