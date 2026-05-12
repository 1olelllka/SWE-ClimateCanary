package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.BuildingCreateDTO;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.services.BuildingService;
import at.qe.skeleton.tests.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BuildingControllerIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired BuildingService buildingService;
    @Autowired BuildingRepository buildingRepository;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        buildingRepository.deleteAll();
    }

    @Test
    void testThatBuildingEndpointsAreSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/buildings"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatGetPageOfBuildingsReturnsHttp200OK() throws Exception {
        buildingService.createBuilding(TestDataUtil.createBuildingEntity());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/buildings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].departments").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatGetSpecificBuildingReturnsHttp200WhenExists() throws Exception {
        Building saved = buildingService.createBuilding(TestDataUtil.createBuildingEntity());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/buildings/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value(saved.getName()));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatGetSpecificBuildingReturnsHttp404WhenNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/buildings/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatCreateNewBuildingReturnsHttp201Created() throws Exception {
        Building building = TestDataUtil.createBuildingEntity();
        BuildingCreateDTO dto = new BuildingCreateDTO(building.getName(), building.getAddress());
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/buildings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(dto.name()))
                .andExpect(jsonPath("$.address").value(dto.address()));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatCreateBuildingWithExistingNameReturnsHttp409Conflict() throws Exception {
        Building existing = buildingService.createBuilding(TestDataUtil.createBuildingEntity());
        BuildingCreateDTO duplicateDto = new BuildingCreateDTO(existing.getName(), "Some other address");
        String json = objectMapper.writeValueAsString(duplicateDto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/buildings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatCreateBuildingWithExistingAddressReturnsHttp409Conflict() throws Exception {
        Building existing = buildingService.createBuilding(TestDataUtil.createBuildingEntity());
        BuildingCreateDTO duplicateDto = new BuildingCreateDTO("New Building 2", existing.getAddress());
        String json = objectMapper.writeValueAsString(duplicateDto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/buildings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatPatchSpecificBuildingUpdatesFieldsSuccessfully() throws Exception {
        Building saved = buildingService.createBuilding(TestDataUtil.createBuildingEntity());
        BuildingCreateDTO patchDto = new BuildingCreateDTO("Updated Name", saved.getAddress());
        String json = objectMapper.writeValueAsString(patchDto);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/buildings/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.address").value(saved.getAddress()));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatPatchBuildingWithDuplicateAddressReturnsHttp409() throws Exception {
        Building building1 = buildingService.createBuilding(Building.builder().name("B1").address("Address 1").build());
        buildingService.createBuilding(Building.builder().name("B2").address("Address 2").build());

        BuildingCreateDTO conflictDto = new BuildingCreateDTO("B1", "Address 2");
        String json = objectMapper.writeValueAsString(conflictDto);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/buildings/" + building1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatDeleteBuildingReturnsHttp204NoContent() throws Exception {
        Building saved = buildingService.createBuilding(TestDataUtil.createBuildingEntity());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/buildings/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/buildings/" + saved.getId()))
                .andExpect(status().isNotFound());
    }
}