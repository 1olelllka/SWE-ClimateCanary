package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.DepartmentDTO;
import at.qe.skeleton.dtos.UserRoom;
import at.qe.skeleton.mappers.DepartmentDetailMapper;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.model.RoomType;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DepartmentDetailMapperUnitTests {

    private final DepartmentDetailMapper mapper = new DepartmentDetailMapper();

    @Test
    void testThatMapToShouldIncludeBuildingAndRoomIds() {
        Building building = TestDataUtil.createBuildingEntity();
        UUID buildingId = UUID.randomUUID();
        building.setId(buildingId);
        building.setName("Technik");

        Department entity = TestDataUtil.createDepartmentEntity(building);
        UUID deptId = UUID.randomUUID();
        entity.setId(deptId);
        entity.setName("Informatics");

        Room room = Room.builder().id(UUID.randomUUID()).build();
        entity.setRooms(List.of(room));

        DepartmentDTO result = mapper.mapTo(entity);

        assertEquals(deptId, result.id());
        assertEquals("Informatics", result.name());
        assertEquals(buildingId.toString(), result.buildingID());
        assertEquals("Technik", result.buildingName());
        assertEquals(1, result.rooms().size());
        assertEquals(entity.getRooms().getFirst().getId(), result.rooms().get(0).id());
    }

    @Test
    void testThatMapToWhenRoomsNullShouldReturnEmptyList() {
        Building building = TestDataUtil.createBuildingEntity();
        building.setId(UUID.randomUUID());
        Department entity = TestDataUtil.createDepartmentEntity(building);
        entity.setRooms(null);

        DepartmentDTO result = mapper.mapTo(entity);

        assertNotNull(result.rooms());
        assertTrue(result.rooms().isEmpty());
    }

    @Test
    void testThatMapFromShouldReconstructEntityWithRooms() {
        UUID buildingId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        DepartmentDTO dto = new DepartmentDTO(deptId, "CS", buildingId.toString(), "Technik",
                List.of(new UserRoom(UUID.randomUUID(), deptId, "CS", RoomType.OFFICE, "123")));

        Department result = mapper.mapFrom(dto);

        assertEquals(deptId, result.getId());
        assertEquals("CS", result.getName());
        assertEquals(buildingId, result.getBuilding().getId());
        assertEquals(1, result.getRooms().size());
        assertEquals(dto.rooms().getFirst().id(), result.getRooms().get(0).getId());
        assertEquals(dto.rooms().getFirst().departmentID(), deptId);
    }
}