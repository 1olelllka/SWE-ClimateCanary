package at.qe.skeleton.services.impl;

import at.qe.skeleton.dtos.UserSettingsPatchDTO;
import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.UserSettings;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.UserSettingsRepository;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link UserService} and Spring's {@link UserDetailsService}.
 * Handles user CRUD, password encoding, and {@link UserSettings} management.
 * A {@link UserSettings} record with default values is created automatically when
 * a new user is registered. Deleting a user also removes the associated settings.
 */
@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private UserxRepository userxRepository;
    private PasswordEncoder passwordEncoder;
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    public UserServiceImpl(UserxRepository userxRepository,
                           PasswordEncoder passwordEncoder,
                           UserSettingsRepository userSettingsRepository) {
        this.userxRepository = userxRepository;
        this.passwordEncoder = passwordEncoder;
        this.userSettingsRepository = userSettingsRepository;
    }

    /**
     * Returns a paginated list of all users.
     *
     * @param pageable pagination parameters
     * @return page of {@link Userx} entities
     */
    @Override
    public Page<Userx> getPageOfUsers(Pageable pageable) {
        return userxRepository.findAll(pageable);
    }

    /**
     * Returns the user with the given ID.
     *
     * @param id the user UUID
     * @return the matching {@link Userx}
     * @throws NotFoundException if no user with that ID exists
     */
    @Override
    public Userx getSpecificUser(UUID id) {
        return userxRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id " + id + " was not found."));
    }

    /**
     * Applies a partial update to an existing user. Only non-null fields are applied.
     * The room assignment is always overwritten — pass {@code null} to unassign the user
     * from their current room.
     *
     * @param id the UUID of the user to update
     * @param u  a partial {@link Userx} carrying the fields to update
     * @return the updated {@link Userx}
     * @throws NotFoundException if no user with that ID exists
     * @throws ConflictException if the new username is already taken by another user
     */
    @Override
    public Userx updateUser(UUID id, Userx u) {
        return userxRepository.findById(id).map(user -> {
            Optional.ofNullable(u.getUsername()).ifPresent(username -> {
                if (!username.equals(user.getUsername()) && userxRepository.existsByUsername(username)) {
                    throw new ConflictException("Username " + username + " not available");
                }
                user.setUsername(username);
            });
            Optional.ofNullable(u.getFirstName()).ifPresent(user::setFirstName);
            Optional.ofNullable(u.getLastName()).ifPresent(user::setLastName);
            Optional.ofNullable(u.getUserRoles()).ifPresent(user::setUserRoles);
            Optional.ofNullable(u.getEnabled()).ifPresent(user::setEnabled);
            user.setMyRoom(u.getMyRoom()); // always apply (null = unassign room)
            return userxRepository.save(user);
        }).orElseThrow(() -> new NotFoundException("User with id " + id + " was not found."));
    }

    /**
     * Creates and persists a new user. The password is encoded before storage.
     * A default {@link UserSettings} record is created for the new user.
     *
     * @param userx the user to create
     * @return the saved {@link Userx}
     * @throws ConflictException if the username is already taken
     */
    @Override
    public Userx createNewUser(Userx userx) {
        if (userxRepository.existsByUsername(userx.getUsername())) {
            throw new ConflictException("Username " + userx.getUsername() + " not available");
        }
        userx.setPassword(passwordEncoder.encode(userx.getPassword()));
        Userx created = userxRepository.save(userx);
        userSettingsRepository.save(UserSettings.builder().userId(created.getId()).build());
        return created;
    }

    /**
     * Returns the settings for the given user.
     *
     * @param id the user UUID
     * @return the user's {@link UserSettings}
     * @throws NotFoundException if no settings record exists for that user
     */
    @Override
    public UserSettings getUserSettings(UUID id) {
        return userSettingsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Settings for this user were not found."));
    }

    /**
     * Applies a partial update to the settings of the given user. Only non-null
     * fields in the DTO are applied.
     *
     * @param id  the user UUID
     * @param dto the settings fields to update
     * @return the updated {@link UserSettings}
     * @throws NotFoundException if no settings record exists for that user
     */
    @Override
    public UserSettings updateUserSettings(UUID id, UserSettingsPatchDTO dto) {
        return userSettingsRepository.findById(id).map(settings -> {
            Optional.ofNullable(dto.darkMode()).ifPresent(settings::setDarkMode);
            Optional.ofNullable(dto.fahrenheit()).ifPresent(settings::setFahrenheit);
            Optional.ofNullable(dto.twelveHourFormat()).ifPresent(settings::setTwelveHourFormat);
            Optional.ofNullable(dto.format()).ifPresent(settings::setFormat);
            Optional.ofNullable(dto.notificationEmail()).ifPresent(settings::setNotificationEmail);
            Optional.ofNullable(dto.emailWarnings()).ifPresent(settings::setEmailWarnings);
            Optional.ofNullable(dto.emailAbsences()).ifPresent(settings::setEmailAbsences);
            return userSettingsRepository.save(settings);
        }).orElseThrow(() -> new NotFoundException("Settings for this user were not found."));
    }

    /**
     * Deletes the user and their associated settings record.
     *
     * @param id the UUID of the user to delete
     */
    @Override
    public void deleteUser(UUID id) {
        userxRepository.deleteById(id);
        userSettingsRepository.deleteById(id);
    }

    /**
     * Returns the user with the given username, including their roles.
     *
     * @param username the username to look up
     * @return the matching {@link Userx}
     * @throws NotFoundException if no user with that username exists
     */
    @Override
    public Userx getByUsername(String username) {
        return userxRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new NotFoundException("User " + username + " was not found."));
    }

    /**
     * Loads a user by username for Spring Security authentication.
     *
     * @param username the username to look up
     * @return the {@link UserDetails} for the given username
     * @throws UsernameNotFoundException if no user with that username exists
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userxRepository.findFirstByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}