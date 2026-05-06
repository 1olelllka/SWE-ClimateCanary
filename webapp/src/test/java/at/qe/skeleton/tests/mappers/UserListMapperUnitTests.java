package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.UserxListDTO;
import at.qe.skeleton.mappers.UserListMapper;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserListMapperUnitTests {

    private final UserListMapper mapper = new UserListMapper();

    @Test
    void testThatMapToShouldIncludeBasicUserInfo() {
        Userx entity = TestDataUtil.createUserxEntity(null, null);
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        entity.setId(userId);
        entity.setCreateDate(now);
        entity.setUsername("list.user");
        entity.setFirstName("List");
        entity.setLastName("User");

        UserxListDTO result = mapper.mapTo(entity);

        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(now, result.createDate());
        assertEquals(entity.getUsername(), result.username());
        assertEquals(entity.getFirstName(), result.firstName());
        assertEquals(entity.getLastName(), result.lastName());
    }

    @Test
    void testThatMapFromShouldReconstructEntityWithBasicFields() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        UserxListDTO dto = new UserxListDTO(
                userId,
                now,
                "jsmith",
                "Jane",
                "Smith"
        );
        Userx result = mapper.mapFrom(dto);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals(now, result.getCreateDate());
        assertEquals("jsmith", result.getUsername());
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());

        assertNull(result.getUserRoles());
        assertNull(result.getMyRoom());
    }
}