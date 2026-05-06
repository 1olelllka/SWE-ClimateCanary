package at.qe.skeleton.tests.services;

import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.model.Permission;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.repositories.RoleRepository;
import at.qe.skeleton.services.impl.UserRoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceUnitTests {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserRoleServiceImpl userRoleService;

    private UserRole sampleRole;
    private UUID roleId;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();
        sampleRole = UserRole.builder()
                .id(roleId)
                .name("TEST")
                .permissions(Set.of(Permission.CAN_MANAGE_OWN_ABSENCE))
                .build();
    }

    @Test
    void testThatGetListOfPermissionsShouldReturnAllRoles() {
        when(roleRepository.findAll()).thenReturn(List.of(sampleRole));
        List<UserRole> result = userRoleService.getListOfPermissions();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TEST", result.get(0).getName());
        verify(roleRepository, times(1)).findAll();
    }

    @Test
    void testThatUpdateExistingPermissionUpdatesFieldsSuccessfully() {
        UserRole updateData = UserRole.builder()
                .name("SUPER_TEST")
                .permissions(Set.of(Permission.CAN_VIEW_ABSENCE_VIEW))
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(sampleRole));
        when(roleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserRole result = userRoleService.updateExistingPermission(roleId, updateData);
        assertEquals("SUPER_TEST", result.getName());
        assertTrue(result.getPermissions().contains(Permission.CAN_VIEW_ABSENCE_VIEW));
        verify(roleRepository).save(any(UserRole.class));
    }

    @Test
    void testThatUpdateExistingPermissionShouldThrowNotFoundWhenIdDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(roleRepository.findById(unknownId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () ->
                userRoleService.updateExistingPermission(unknownId, new UserRole())
        );
        verify(roleRepository, never()).save(any());
    }

    @Test
    void testThatUpdateExistingPermissionDoesNotUpdateFieldsWhenDtoValuesAreNull() {
        UserRole emptyDto = new UserRole();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(sampleRole));
        when(roleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserRole result = userRoleService.updateExistingPermission(roleId, emptyDto);

        assertEquals("TEST", result.getName());
        assertEquals(1, result.getPermissions().size());
    }
}