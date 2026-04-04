package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.UserxCreateDTO;
import at.qe.skeleton.mappers.UserxCreateMapper;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserxCreateMapperUnitTests {

    private final UserxCreateMapper mapper = new UserxCreateMapper();

    @Test
    void testThatMapFromShouldReconstructEntityWithPassword() {
        UserxCreateDTO dto = TestDataUtil.createUserxCreateDTO(Set.of());

        Userx result = mapper.mapFrom(dto);

        assertNotNull(result);
        assertEquals(dto.username(), result.getUsername());
        assertEquals(dto.password(), result.getPassword());
        assertEquals(dto.firstName(), result.getFirstName());
        assertEquals(dto.lastName(), result.getLastName());
        assertTrue(result.isEnabled());
    }

    @Test
    void testThatMapToShouldThrowUnsupportedOperationException() {
        Userx entity = new Userx();

        assertThrows(UnsupportedOperationException.class, () -> mapper.mapTo(entity));
    }
}