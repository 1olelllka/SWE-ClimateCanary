package at.qe.skeleton.services;

import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.UserxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for accessing currently authenticated user.
 *
 */
@Service
public class AuthenticatedUserService {

    private final UserxRepository userRepository;

    @Autowired
    public AuthenticatedUserService(UserxRepository userxRepository) {
        this.userRepository = userxRepository;
    }

    /**
     * Returns the currently authenticated user.
     *
     * @return the authenticated user or null
     */
    public Userx getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findFirstByUsername(auth.getName()).orElseThrow(() -> new NotFoundException("User with username " + auth.getName() + " was not found."));
    }

}
