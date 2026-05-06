/**
 * Spring configuration for web security.
 * <p>
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by Innsbruck University.
 */

package at.qe.skeleton.services;

import at.qe.skeleton.configs.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Autowired
    public AuthenticationService(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Authenticates a user with the given username and password.
     * @param username the username
     * @param password the password
     * @return the authentication object
     */
    public Authentication authenticateLoginRequest(String username, String password) {
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }

    /**
     * Generates a JWT token for the given authentication object.
     * @param authentication the authentication object
     * @return the generated JWT token
     */
    public String generateToken(Authentication authentication) {
        return tokenProvider.generate(authentication);
    }

}
