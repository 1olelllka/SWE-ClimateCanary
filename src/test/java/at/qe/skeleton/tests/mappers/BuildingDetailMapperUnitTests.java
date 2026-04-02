package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.BuildingDTO;
import at.qe.skeleton.dtos.BuildingDepartmentsDTO;
import at.qe.skeleton.mappers.BuildingDetailMapper;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BuildingDetailMapperUnitTests {

    private final BuildingDetailMapper mapper = new BuildingDetailMapper();

    @Test
    void testThatMapToShouldIncludeDepartments() {
        Building entity = TestDataUtil.createBuildingEntity();
        UUID buildingId = UUID.randomUUID();
        entity.setId(buildingId);
        entity.setName("Technik");

        Department dept = Department.builder().id(UUID.randomUUID()).name("CS").build();
        entity.setDepartments(List.of(dept));

        BuildingDTO result = mapper.mapTo(entity);

        assertEquals(buildingId, result.id());
        assertEquals("Technik", result.name());
        assertEquals(1, result.departments().size());
        assertEquals("CS", result.departments().get(0).name());
    }

    @Test
    void testThatMapToShouldReturnEmptyListWhenDepartmentsNull() {
        Building entity = TestDataUtil.createBuildingEntity();
        entity.setDepartments(null);

        BuildingDTO result = mapper.mapTo(entity);

        assertNotNull(result.departments());
        assertTrue(result.departments().isEmpty());
    }

    @Test
    void testThatMapFromShouldMapFieldsBackToEntity() {
        UUID buildingId = UUID.randomUUID();
        BuildingDepartmentsDTO deptDto = new BuildingDepartmentsDTO(UUID.randomUUID(), "Math");
        BuildingDTO dto = new BuildingDTO(buildingId, "Test Address", "Test Building", List.of(deptDto));

        Building result = mapper.mapFrom(dto);

        assertEquals(buildingId, result.getId());
        assertEquals("Test Building", result.getName());
        assertEquals(1, result.getDepartments().size());
        assertEquals(buildingId, result.getDepartments().get(0).getBuilding().getId());
    }
}