package at.qe.skeleton.tests.services;

import at.qe.skeleton.exceptions.ConflictException;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.services.impl.UserServiceImpl;
import at.qe.skeleton.tests.TestDataUtil;
import lombok.extern.java.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTests {

    @Mock
    private UserxRepository userxRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private Userx sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = TestDataUtil.createUserxEntity(null, null);
        sampleUser.setId(userId);
        sampleUser.setPassword("password");
    }

    @Test
    void testThatGetPageOfUsersShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Userx> page = new PageImpl<>(List.of(sampleUser));
        when(userxRepository.findAll(pageable)).thenReturn(page);

        Page<Userx> result = userService.getPageOfUsers(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userxRepository, times(1)).findAll(pageable);
    }

    @Test
    void testThatGetSpecificUserWhenExistsShouldReturnUser() {
        when(userxRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        Userx result = userService.getSpecificUser(userId);

        assertEquals(sampleUser.getUsername(), result.getUsername());
        assertEquals(userId, result.getId());
    }

    @Test
    void testThatGetSpecificUserWhenNotExistsShouldThrowNotFoundException() {
        when(userxRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.getSpecificUser(userId));
    }

    @Test
    void testThatCreateNewUserSuccessfulAndEncodesPassword() {
        when(userxRepository.existsByUsername(sampleUser.getUsername())).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userxRepository.save(any(Userx.class))).thenReturn(sampleUser);

        Userx result = userService.createNewUser(sampleUser);

        assertNotNull(result);
        assertEquals("encodedPassword", sampleUser.getPassword()); // Verify password was altered
        verify(passwordEncoder, times(1)).encode("password");
        verify(userxRepository, times(1)).save(sampleUser);
    }

    @Test
    void testThatCreateNewUserShouldThrowConflictIfUsernameExists() {
        when(userxRepository.existsByUsername(sampleUser.getUsername())).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.createNewUser(sampleUser));

        verify(passwordEncoder, never()).encode(anyString());
        verify(userxRepository, never()).save(any());
    }

    @Test
    void testThatUpdateUserUpdatesFieldsSuccessfully() {
        Userx patchData = Userx.builder().username("new.username").firstName("NewFirst").enabled(false).build();

        when(userxRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userxRepository.save(any(Userx.class))).thenAnswer(a -> a.getArgument(0));

        Userx result = userService.updateUser(userId, patchData);

        assertEquals("new.username", result.getUsername());
        assertEquals("NewFirst", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertFalse(result.getEnabled());
        verify(userxRepository, times(1)).save(any(Userx.class));
    }

    @Test
    void testThatUpdateUserShouldThrowNotFoundWhenIdDoesNotExist() {
        when(userxRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.updateUser(userId, new Userx()));
    }

    @Test
    void testThatDeleteUserShouldCallRepository() {
        userService.deleteUser(userId);
        verify(userxRepository, times(1)).deleteById(userId);
    }
}