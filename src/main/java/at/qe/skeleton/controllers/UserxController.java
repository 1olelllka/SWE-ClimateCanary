package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.UserxDTO;
import at.qe.skeleton.mappers.UserxMapper;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.AuthenticatedUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Userx endpoints exposed by the server.
 *
 * This class is part of the skeleton project provided for students of the
* course "Software Engineering" offered by Innsbruck University.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserxController {
 
    private final UserxMapper userMapper;
    private final AuthenticatedUserService authenticatedUserService;

    @Autowired
    public UserxController(UserxMapper userMapper, AuthenticatedUserService authenticatedUserService) {
        this.userMapper = userMapper;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Operation(summary = "Get current user",
            description = "Get the currently authenticated user.")
    @ApiResponse(responseCode = "200", description = "The currently authenticated user.")
    @ApiResponse(responseCode = "401", description = "User not authenticated.")
    @ApiResponse(responseCode = "403", description = "User not authorized.")
    @GetMapping("/me")
    public ResponseEntity<UserxDTO> getCurrentUser() {
        Userx authenticatedUser = authenticatedUserService.getAuthenticatedUser();
        return ResponseEntity.ok(userMapper.mapTo(authenticatedUser));
    }
    @Operation(summary = "Check if user is authenticated",
            description = "Check if the user is authenticated.")
    @GetMapping("/authenticated")
    public ResponseEntity<String> isAuthenticated(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        return ResponseEntity.ok("User is authenticated: " + userDetails.getUsername());
        
    }
}
