package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.RoomDTO;
import at.qe.skeleton.dtos.UserxListDTO;
import at.qe.skeleton.model.*;
import at.qe.skeleton.mappers.RoomMapper;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RoomMapperUnitTests {

    private final RoomMapper mapper = new RoomMapper();

    @Test
    void testThatMapToShouldIncludeDepartmentInfoAndRoomDetails() {
        Building building = TestDataUtil.createBuildingEntity();
        Department department = TestDataUtil.createDepartmentEntity(building);
        UUID deptId = UUID.randomUUID();
        department.setId(deptId);
        department.setName("Informatics Dept");

        Room entity = TestDataUtil.createRoomEntity(department);
        UUID roomId = UUID.randomUUID();
        entity.setId(roomId);
        entity.setRoomType(RoomType.OFFICE);
        entity.setIsActive(true);
        entity.setDefaultPeopleCnt(15);

        RoomDTO result = mapper.mapTo(entity);

        assertEquals(roomId, result.id());
        assertEquals(deptId, result.departmentID());
        assertEquals("Informatics Dept", result.departmentName());
        assertEquals(RoomType.OFFICE, result.roomType());
        assertTrue(result.isActive());
        assertEquals(15, result.defaultPeopleCount());
    }

    @Test
    void testThatMapFromShouldReconstructEntityWithDepartmentId() {
        UUID roomId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();

        RoomDTO dto = new RoomDTO(
                roomId,
                deptId,
                "Informatics Dept",
                false,
                RoomType.OFFICE,
                4,
                Set.of(new UserxListDTO(UUID.randomUUID(), LocalDateTime.now(), "", "", "")),
                "Test"
        );

        Room result = mapper.mapFrom(dto);

        assertEquals(roomId, result.getId());
        assertEquals(RoomType.OFFICE, result.getRoomType());
        assertFalse(result.getIsActive());
        assertEquals(4, result.getDefaultPeopleCnt());

        // Verify that the department shell is created with the correct ID
        assertNotNull(result.getDepartment());
        assertEquals(deptId, result.getDepartment().getId());
        assertEquals(1, result.getUsers().size());
    }
}