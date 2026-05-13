package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.UserRoleCreateDTO;
import at.qe.skeleton.dtos.UserRoleDTO;
import at.qe.skeleton.mappers.UserRoleMapper;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.services.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
public class UserRoleController {

    private UserRoleService userRoleService;
    private UserRoleMapper userRoleMapper;

    @Autowired
    public UserRoleController(UserRoleService userRoleService,
                              UserRoleMapper userRoleMapper) {
        this.userRoleService = userRoleService;
        this.userRoleMapper = userRoleMapper;
    }

    @GetMapping("")
    public ResponseEntity<List<UserRoleDTO>> getAllPermissions() {
        List<UserRole> roles = userRoleService.getListOfPermissions();
        return new ResponseEntity<>(roles.stream().map(userRoleMapper::mapTo).toList(), HttpStatus.OK);
    }

    @PatchMapping("/{role_id}")
    public ResponseEntity<UserRoleDTO> updatePermission(@PathVariable(name = "role_id") UUID id,
                                                        @RequestBody UserRoleCreateDTO dto) {
        UserRole updated = userRoleService.updateExistingPermission(id, UserRole.builder().name(dto.name()).permissions(dto.permissions()).build());
        return new ResponseEntity<>(userRoleMapper.mapTo(updated), HttpStatus.OK);
    }

}
