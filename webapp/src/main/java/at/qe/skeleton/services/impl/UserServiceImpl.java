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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

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

    @Override
    public Page<Userx> getPageOfUsers(Pageable pageable) {
        return userxRepository.findAll(pageable);
    }

    @Override
    public Userx getSpecificUser(UUID id) {
        return userxRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with id " + id + " was not found."));
    }

    @Override
    public Userx updateUser(UUID id, Userx u) {
        return userxRepository.findById(id).map(user -> {
            Optional.ofNullable(u.getUsername()).ifPresent(user::setUsername);
            Optional.ofNullable(u.getFirstName()).ifPresent(user::setFirstName);
            Optional.ofNullable(u.getLastName()).ifPresent(user::setLastName);
            Optional.ofNullable(u.getUserRoles()).ifPresent(user::setUserRoles);
            Optional.ofNullable(u.getEnabled()).ifPresent(user::setEnabled);
            user.setMyRoom(u.getMyRoom()); // always apply (null = unassign room)
            return userxRepository.save(user);
        }).orElseThrow(() -> new NotFoundException("User with id " + id + " was not found."));
    }

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

    @Override
    public UserSettings getUserSettings(UUID id) {
        return userSettingsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Settings for this user were not found."));
    }

    @Override
    public UserSettings updateUserSettings(UUID id, UserSettingsPatchDTO dto) {
        return userSettingsRepository.findById(id).map(settings -> {
            Optional.ofNullable(dto.darkMode()).ifPresent(settings::setDarkMode);
            Optional.ofNullable(dto.fahrenheit()).ifPresent(settings::setFahrenheit);
            Optional.ofNullable(dto.twelveHourFormat()).ifPresent(settings::setTwelveHourFormat);
            Optional.ofNullable(dto.format()).ifPresent(settings::setFormat);
            return userSettingsRepository.save(settings);
        }).orElseThrow(() -> new NotFoundException("Settings for this user were not found."));
    }

    @Override
    public void deleteUser(UUID id) {
        userxRepository.deleteById(id);
        userSettingsRepository.deleteById(id);
    }

    @Override
    public Userx getByUsername(String username) {
        return userxRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new NotFoundException("User " + username + " was not found."));
    }

}
