package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.BuildingListDTO;
import at.qe.skeleton.mappers.BuildingListMapper;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.tests.TestDataUtil;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BuildingListMapperUnitTests {

    private final BuildingListMapper mapper = new BuildingListMapper();

    @Test
    void testThatMapToShouldOnlyIncludeBasicFields() {
        Building entity = TestDataUtil.createBuildingEntity();
        UUID buildingId = UUID.randomUUID();
        entity.setId(buildingId);
        entity.setName("Main Campus");

        BuildingListDTO result = mapper.mapTo(entity);

        assertEquals(buildingId, result.id());
        assertEquals("Main Campus", result.name());
        assertEquals("Test Address", result.address()); // From TestDataUtil
    }

    @Test
    void testThatMapFromShouldReconstructEntity() {
        UUID buildingId = UUID.randomUUID();
        BuildingListDTO dto = new BuildingListDTO(buildingId, "Specific Name", "Specific Address");

        Building result = mapper.mapFrom(dto);

        assertEquals(buildingId, result.getId());
        assertEquals("Specific Name", result.getName());
        assertEquals("Specific Address", result.getAddress());
    }
}