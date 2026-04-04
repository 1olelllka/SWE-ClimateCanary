package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.UserxPatchDTO;
import at.qe.skeleton.mappers.UserPatchMapper;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserPatchMapperUnitTests {

    private final UserPatchMapper mapper = new UserPatchMapper();

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
    void testThatMapFromShouldReconstructEntityWithRoleShells() {
        UUID roleId = UUID.randomUUID();
        UserxPatchDTO dto = new UserxPatchDTO(
                "jsmith",
                "Jane",
                "Smith",
                true,
                Set.of(roleId)
        );

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
        UserxPatchDTO dto = new UserxPatchDTO("user", "F", "L", true, null);

        Userx result = mapper.mapFrom(dto);

        assertNull(result.getUserRoles());
    }
}