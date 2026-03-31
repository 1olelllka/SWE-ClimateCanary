package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.DepartmentListDTO;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.mappers.DepartmentListMapper;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DepartmentListMapperUnitTests {

    private final DepartmentListMapper mapper = new DepartmentListMapper();

    @Test
    void testThatMapToShouldOnlyIncludeBasicFieldsAndBuildingInfo() {
        Building building = TestDataUtil.createBuildingEntity();
        UUID buildingId = UUID.randomUUID();
        building.setId(buildingId);
        building.setName("Main Campus");

        Department entity = TestDataUtil.createDepartmentEntity(building);
        UUID deptId = UUID.randomUUID();
        entity.setId(deptId);
        entity.setName("History");

        DepartmentListDTO result = mapper.mapTo(entity);

        assertEquals(deptId, result.id());
        assertEquals("History", result.name());
        assertEquals(buildingId.toString(), result.buildingID());
        assertEquals("Main Campus", result.buildingName());
    }

    @Test
    void testThatMapFromShouldReconstructEntityWithoutRooms() {
        UUID buildingId = UUID.randomUUID();
        UUID deptId = UUID.randomUUID();
        DepartmentListDTO dto = new DepartmentListDTO(deptId, "Physics", buildingId.toString(), "Main Campus");

        Department result = mapper.mapFrom(dto);

        assertEquals(deptId, result.getId());
        assertEquals("Physics", result.getName());
        assertEquals(buildingId, result.getBuilding().getId());
        assertNull(result.getRooms());
    }
}