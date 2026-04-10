package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.UserxCreateDTO;
import at.qe.skeleton.mappers.UserxCreateMapper;
import at.qe.skeleton.model.UserRole;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.RoleRepository;
import at.qe.skeleton.tests.TestDataUtil;
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
class UserxCreateMapperUnitTests {

    @Mock
    private RoleRepository roleRepository;
    @InjectMocks
    private UserxCreateMapper mapper;

    @Test
    void testThatMapFromShouldReconstructEntityWithPassword() {
        UserxCreateDTO dto = TestDataUtil.createUserxCreateDTO(Set.of(UUID.randomUUID()));

        dto.roles().forEach(role -> {
            when(roleRepository.getReferenceById(role)).thenReturn(UserRole.builder().id(role).build());
        });
        Userx result = mapper.mapFrom(dto);

        assertNotNull(result);
        assertEquals(dto.username(), result.getUsername());
        assertEquals(dto.password(), result.getPassword());
        assertEquals(dto.firstName(), result.getFirstName());
        assertEquals(dto.lastName(), result.getLastName());
        assertEquals(1, result.getUserRoles().size());
        assertTrue(result.isEnabled());
    }

    @Test
    void testThatMapToShouldThrowUnsupportedOperationException() {
        Userx entity = new Userx();

        assertThrows(UnsupportedOperationException.class, () -> mapper.mapTo(entity));
    }
}