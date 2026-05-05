package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.UserRoleDTO;
import at.qe.skeleton.mappers.UserRoleMapper;
import at.qe.skeleton.model.Permission;
import at.qe.skeleton.model.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserRoleMapperUnitTests {

    private final UserRoleMapper mapper = new UserRoleMapper();

    @Test
    void testThatMapToShouldIncludeNameAndPermissions() {
        UserRole entity = UserRole.builder()
                .id(UUID.randomUUID())
                .name("TEST")
                .permissions(Set.of(Permission.CAN_VIEW_ABSENCE_VIEW, Permission.CAN_MANAGE_OWN_ABSENCE, Permission.CAN_VIEW_ALL_ROOMS))
                .build();
        UserRoleDTO result = mapper.mapTo(entity);
        assertEquals(entity.getId(), result.id());
        assertEquals("TEST", result.name());
        assertEquals(3, result.permissions().size());
        assertTrue(result.permissions().contains(Permission.CAN_VIEW_ABSENCE_VIEW));
    }

    @Test
    void testThatMapFromShouldReconstructEntityWithPermissions() {
        UserRoleDTO dto = new UserRoleDTO(UUID.randomUUID(), "TEST", Set.of(Permission.CAN_MANAGE_OWN_ABSENCE));
        UserRole result = mapper.mapFrom(dto);
        assertEquals(dto.id(), result.getId());
        assertEquals("TEST", result.getName());
        assertNotNull(result.getPermissions());
        assertEquals(1, result.getPermissions().size());
        assertTrue(result.getPermissions().contains(Permission.CAN_MANAGE_OWN_ABSENCE));
    }

    @Test
    void testThatMapToWhenPermissionsNullShouldReturnNull() {
        UserRole entity = UserRole.builder()
                .id(UUID.randomUUID())
                .name("TEST")
                .permissions(null)
                .build();

        UserRoleDTO result = mapper.mapTo(entity);

        assertNull(result.permissions());
    }
}