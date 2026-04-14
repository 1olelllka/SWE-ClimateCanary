package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.UserRoleDTO;
import at.qe.skeleton.dtos.UserRoom;
import at.qe.skeleton.dtos.UserxDTO;
import at.qe.skeleton.mappers.UserRoleMapper;
import at.qe.skeleton.mappers.UserxMapper;
import at.qe.skeleton.model.*;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserxMapperUnitTests {

    @Mock
    private UserRoleMapper roleMapper;

    @InjectMocks
    private UserxMapper userxMapper;

    private Userx sampleUser;
    private UserRole sampleRole;
    private UserRoleDTO sampleRoleDto;

    @BeforeEach
    void setUp() {
        sampleRole = TestDataUtil.createUserRole(Set.of());
        sampleRoleDto = new UserRoleDTO(UUID.randomUUID(), sampleRole.getName(), Set.of());

        Department dept = TestDataUtil.createDepartmentEntity(null);
        Room room = TestDataUtil.createRoomEntity(dept);

        sampleUser = TestDataUtil.createUserxEntity(sampleRole, room);
    }

    @Test
    void testThatMapToShouldIncludeUserInfoAndMappedRoles() {
        when(roleMapper.mapTo(sampleRole)).thenReturn(sampleRoleDto);

        UserxDTO result = userxMapper.mapTo(sampleUser);

        assertNotNull(result);
        assertEquals(sampleUser.getUsername(), result.username());
        assertEquals(sampleUser.getFirstName(), result.firstName());
        assertEquals(sampleUser.getUserRoles().size(), result.roles().size());
        assertNotNull(result.myRoom());
        assertEquals(sampleUser.getMyRoom().getDepartment().getName(), result.myRoom().departmentName());
    }

    @Test
    void testThatMapToWhenUserIsNullShouldReturnNull() {
        UserxDTO result = userxMapper.mapTo(null);
        assertNull(result);
    }

    @Test
    void testThatMapToWhenRoomIsNullShouldReturnNullRoomInDto() {
        sampleUser.setMyRoom(null);
        when(roleMapper.mapTo(any())).thenReturn(sampleRoleDto);
        UserxDTO result = userxMapper.mapTo(sampleUser);
        assertNull(result.myRoom());
    }

    @Test
    void testThatMapFromShouldReconstructUserWithShellRoom() {
        UUID roomId = UUID.randomUUID();
        UserRoom userRoom = new UserRoom(roomId, UUID.randomUUID(), "IT", RoomType.OFFICE);
        UserxDTO dto = new UserxDTO(
                UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(),
                "jdoe", "John", "Doe", true, null,
                Set.of(sampleRoleDto), userRoom
        );

        when(roleMapper.mapFrom(sampleRoleDto)).thenReturn(sampleRole);

        Userx result = userxMapper.mapFrom(dto);
        assertEquals(dto.firstName(), result.getFirstName());
        assertTrue(result.isEnabled());
        assertNotNull(result.getMyRoom());
        assertEquals(roomId, result.getMyRoom().getId());
        assertEquals(dto.roles().size(), result.getUserRoles().size());
    }

    @Test
    void testThatMapFromWhenRoomDtoIsNullShouldReturnNullRoomInEntity() {
        UserxDTO dto = new UserxDTO(
                UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(),
                "jdoe", "John", "Doe", true, null,
                Set.of(sampleRoleDto), null
        );
        when(roleMapper.mapFrom(any())).thenReturn(sampleRole);
        Userx result = userxMapper.mapFrom(dto);
        assertNull(result.getMyRoom());
    }
}