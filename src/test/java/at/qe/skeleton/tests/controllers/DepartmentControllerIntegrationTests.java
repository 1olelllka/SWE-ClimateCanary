package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.DepartmentCreateDTO;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.services.DepartmentService;
import at.qe.skeleton.tests.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DepartmentControllerIntegrationTests {

    private final MockMvc mockMvc;
    private final DepartmentService departmentService;
    private final BuildingRepository buildingRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public DepartmentControllerIntegrationTests(DepartmentService departmentService,
                                                BuildingRepository buildingRepository,
                                                MockMvc mockMvc) {
        this.departmentService = departmentService;
        this.buildingRepository = buildingRepository;
        this.mockMvc = mockMvc;
        this.objectMapper = new ObjectMapper();
    }

    @Test
    public void testThatGetPageOfDepartmentsReturnsHttp200OK() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));

        mockMvc.perform(MockMvcRequestBuilders.get("/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].rooms").doesNotExist()); // Verification for ListDTO
    }

    @Test
    public void testThatGetSpecificDepartmentReturnsHttp200WhenExists() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department saved = departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));

        mockMvc.perform(MockMvcRequestBuilders.get("/departments/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value(saved.getName()));
    }

    @Test
    public void testThatGetSpecificDepartmentReturnsHttp404WhenNotExist() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/departments/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testThatCreateNewDepartmentReturnsHttp201Created() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        DepartmentCreateDTO dto = new DepartmentCreateDTO("New Dept", b.getId());
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Dept"));
    }

    @Test
    public void testThatCreateNewDepartmentReturnsHttp409ConflictIfSameNameExists() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));
        DepartmentCreateDTO dto = new DepartmentCreateDTO(d.getName(), b.getId());
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    public void testThatPatchSpecificDepartmentUpdatesFieldsSuccessfully() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department saved = departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));

        DepartmentCreateDTO patchDto = new DepartmentCreateDTO("Updated Dept", b.getId());
        String json = objectMapper.writeValueAsString(patchDto);

        mockMvc.perform(MockMvcRequestBuilders.patch("/departments/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Dept"));
    }

    @Test
    public void testThatPatchSpecificDepartmentReturnsHttp409Conflict() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department saved = departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));
        Department second = TestDataUtil.createDepartmentEntity(b);
        second.setName("Test");
        departmentService.createDepartment(second);

        DepartmentCreateDTO patchDto = new DepartmentCreateDTO(second.getName(), b.getId());
        String json = objectMapper.writeValueAsString(patchDto);

        mockMvc.perform(MockMvcRequestBuilders.patch("/departments/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    public void testThatDeleteDepartmentReturnsHttp204NoContent() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department saved = departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));

        mockMvc.perform(MockMvcRequestBuilders.delete("/departments/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get("/departments/" + saved.getId()))
                .andExpect(status().isNotFound());
    }
}