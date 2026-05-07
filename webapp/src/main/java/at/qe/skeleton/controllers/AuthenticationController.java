/**
 * Spring configuration for web security.
 * <p>
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by Innsbruck University.
 */

package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.LoginRequestDTO;
import at.qe.skeleton.dtos.LoginResponseDTO;
import at.qe.skeleton.exceptions.ValidationException;
import at.qe.skeleton.services.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Autowired
    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param loginRequest the login request containing the username and password
     * @return the JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> authenticateUser(@RequestBody @Valid LoginRequestDTO loginRequest,
                                                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream().map(err -> err.getDefaultMessage()).collect(Collectors.joining(" "));
            throw new ValidationException(msg);
        }
        Authentication authentication = authenticationService.authenticateLoginRequest(loginRequest.username(), loginRequest.password());
        String token = authenticationService.generateToken(authentication);
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }
}
