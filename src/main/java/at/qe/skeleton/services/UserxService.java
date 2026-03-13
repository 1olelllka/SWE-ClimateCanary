package at.qe.skeleton.services;

import at.qe.skeleton.exceptions.UsernameDuplicateException;
import at.qe.skeleton.model.Userx;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import at.qe.skeleton.repositories.UserxRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for accessing and manipulating user data.
 *
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by Innsbruck University.
 */
@Service
public class UserxService implements UserDetailsService {
 
    private final UserxRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserService authenticatedUserService;

    @Autowired
    public UserxService(UserxRepository userRepository, PasswordEncoder passwordEncoder, AuthenticatedUserService authenticatedUserService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticatedUserService = authenticatedUserService;
    }
    
    /**
     * Returns a collection of all users.
     *
     * @return the userx collection
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    public Collection<Userx> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Loads a single user identified by its id.
     *
     * @param id the id to search for
     * @return the user with the id
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    public Optional<Userx> loadUser(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Saves the user. This method will also set {@link Userx#createDate} for new
     * entities or {@link Userx#updateDate} for updated entities. The user
     * requesting this operation will also be stored as {@link Userx#createDate}
     * or {@link Userx#updateUser} respectively.
     *
     * @param user the user to save
     * @return the updated user
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    public Userx saveUser(Userx user) {
        if (user.isNew()) {
            if (userRepository.existsByUsername(user.getUsername())) {
                throw new UsernameDuplicateException("Username " + user.getUsername() + " not available");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setCreateUser(authenticatedUserService.getAuthenticatedUser());
        } else {
            user.setUpdateUser(authenticatedUserService.getAuthenticatedUser());
        }
        return userRepository.save(user);
    }

    /**
     * Deletes the user.
     *
     * @param user the user to delete
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteUser(Userx user) {
        Optional<Userx> userOpt = userRepository.findById(user.getId());
        userOpt.ifPresent(userRepository::delete);
    }

    public Userx getUserByUsername(String username) {
        return userRepository.findFirstByUsername(username).orElse(null);
    }


    /**
     * Loads a user by its username. Required for JWT authentication.
     *
     * @param username the username identifying the user whose data is required.
     * @return the user with the given username and their details.
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
