package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.UserxPatchDTO;
import at.qe.skeleton.exceptions.NotFoundException;
import at.qe.skeleton.mappers.UserPatchMapper;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.RoleRepository;
import at.qe.skeleton.tests.TestDataUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPatchMapperUnitTests {

    @Mock
    private RoleRepository roleRepository;
    @InjectMocks
    private UserPatchMapper mapper;

    @Test
    void testThatMapToShouldIncludeFieldsAndRoleIds() {
        UUID roleId = UUID.randomUUID();
        UserRole role = UserRole.builder().id(roleId).name("ADMIN").build();

        Userx entity = TestDataUtil.createUserxEntity(role, null);
        entity.setUsername("patch.user");
        entity.setFirstName("Patch");
        entity.setEnabled(false);

        UserxPatchDTO result = mapper.mapTo(entity);

        assertNotNull(result);
        assertEquals(entity.getUsername(), result.username());
        assertEquals(entity.getFirstName(), result.firstName());
        assertFalse(result.isEnabled());
        assertEquals(entity.getUserRoles().size(), result.roles().size());
        assertTrue(result.roles().contains(roleId));
    }

    @Test
    void testThatMapToWhenRolesNullShouldReturnNullRolesInDto() {
        Userx entity = TestDataUtil.createUserxEntity(null, null);
        entity.setUserRoles(null);

        UserxPatchDTO result = mapper.mapTo(entity);

        assertNull(result.roles());
    }

    @Test
    void testThatMapFromWhenRolesNotFoundThrowsNotFoundException() {
        UUID roleId = UUID.randomUUID();
        UserxPatchDTO dto = new UserxPatchDTO(
                "jsmith",
                "Jane",
                "Smith",
                true,
                Set.of(roleId),
                null
        );
        when(roleRepository.getReferenceById(roleId)).thenReturn(UserRole.builder().id(roleId).build());
        when(roleRepository.getReferenceById(roleId)).thenThrow(EntityNotFoundException.class);
        assertThrows(NotFoundException.class, () -> mapper.mapFrom(dto));
    }

    @Test
    void testThatMapFromShouldReconstructEntityWithRoleShells() {
        UUID roleId = UUID.randomUUID();
        UserxPatchDTO dto = new UserxPatchDTO(
                "jsmith",
                "Jane",
                "Smith",
                true,
                Set.of(roleId),
                null
        );
        when(roleRepository.getReferenceById(roleId)).thenReturn(UserRole.builder().id(roleId).build());
        Userx result = mapper.mapFrom(dto);

        assertNotNull(result);
        assertEquals(dto.username(), result.getUsername());
        assertEquals(dto.firstName(), result.getFirstName());
        assertTrue(result.isEnabled());

        assertNotNull(result.getUserRoles());
        assertEquals(1, result.getUserRoles().size());
        UserRole mappedRole = result.getUserRoles().iterator().next();
        assertEquals(roleId, mappedRole.getId());
    }

    @Test
    void testThatMapFromWhenRolesDtoIsNullShouldReturnNullRolesInEntity() {
        UserxPatchDTO dto = new UserxPatchDTO("user", "F", "L", true, null, null);

        Userx result = mapper.mapFrom(dto);

        assertNull(result.getUserRoles());
    }
}