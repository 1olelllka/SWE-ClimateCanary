package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.UserRoleCreateDTO;
import at.qe.skeleton.dtos.UserRoleDTO;
import at.qe.skeleton.mappers.UserRoleMapper;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.services.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "User roles management")
public class UserRoleController {

    private UserRoleService userRoleService;
    private UserRoleMapper userRoleMapper;

    @Autowired
    public UserRoleController(UserRoleService userRoleService,
                              UserRoleMapper userRoleMapper) {
        this.userRoleService = userRoleService;
        this.userRoleMapper = userRoleMapper;
    }

    @Operation(summary = "Get all permissions. One of Permissions Required: CAN_MANAGE_USERS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of roles."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @GetMapping("")
    public ResponseEntity<List<UserRoleDTO>> getAllPermissions() {
        List<UserRole> roles = userRoleService.getListOfPermissions();
        return new ResponseEntity<>(roles.stream().map(userRoleMapper::mapTo).toList(), HttpStatus.OK);
    }

    @Operation(summary = "Update specific permission. One of Permissions Required: CAN_MANAGE_USERS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400", description = "Validation issue.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Role not found.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized.", content = @Content())
    })
    @PatchMapping("/{role_id}")
    public ResponseEntity<UserRoleDTO> updatePermission(@PathVariable(name = "role_id") UUID id,
                                                        @RequestBody UserRoleCreateDTO dto) {
        UserRole updated = userRoleService.updateExistingPermission(id, UserRole.builder().name(dto.name()).permissions(dto.permissions()).build());
        return new ResponseEntity<>(userRoleMapper.mapTo(updated), HttpStatus.OK);
    }

}
