package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.DepartmentCreateDTO;
import at.qe.skeleton.dtos.DepartmentWithRoomsCreateDTO;
import at.qe.skeleton.dtos.NewRoomInDepartmentDTO;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.DepartmentService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DepartmentControllerIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired DepartmentService departmentService;
    @Autowired BuildingRepository buildingRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired DepartmentRepository departmentRepository;
    @MockitoBean
    AuthenticatedUserService authenticatedUserService;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        roomRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    @Test
    void testThatDepartmentEndpointsAreSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/departments"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatGetPageOfDepartmentsReturnsHttp200OK() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].rooms").doesNotExist()); // Verification for ListDTO
    }

    @Test
    @WithMockUser(roles = "DEPARTMENT_MANAGER")
    void testThatGetSpecificDepartmentReturnsHttp200WhenExistsWithAllRooms() throws Exception {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(TestDataUtil.createUserxEntity(UserRole.builder().permissions(Set.of(Permission.CAN_VIEW_OWN_DEPARTMENT_MEASURES)).build(), null));
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department saved = departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));
        roomRepository.save(TestDataUtil.createRoomEntity(saved));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/departments/" + saved.getId()+"?onlyShared=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value(saved.getName()))
                .andExpect(jsonPath("$.rooms[0]").exists())
                .andExpect(jsonPath("$.rooms[1]").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "DEPARTMENT_MANAGER")
    void testThatGetSpecificDepartmentReturnsHttp200WhenExistsWithSharedRooms() throws Exception {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(TestDataUtil.createUserxEntity(UserRole.builder().permissions(Set.of(Permission.CAN_VIEW_OWN_DEPARTMENT_MEASURES)).build(), null));
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department saved = departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));
        roomRepository.save(TestDataUtil.createRoomEntity(saved));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/departments/" + saved.getId()+"?onlyShared=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value(saved.getName()))
                .andExpect(jsonPath("$.rooms[0]").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testThatGetSpecificDepartmentReturnsHttp403ForbiddenIfEmployeeWantsToGetAllRooms() throws Exception {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(TestDataUtil.createUserxEntity(UserRole.builder().permissions(Set.of()).build(), null));
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department saved = departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));
        roomRepository.save(TestDataUtil.createRoomEntity(saved));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/departments/" + saved.getId()+"?onlyShared=false"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DEPARTMENT_MANAGER")
    void testThatGetSpecificDepartmentReturnsHttp404WhenNotExist() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/departments/" + UUID.randomUUID()+"?onlyShared=false"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatCreateNewDepartmentReturnsHttp400BadRequestIfInvalid() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        DepartmentCreateDTO dto = new DepartmentCreateDTO("", b.getId());
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatCreateNewDepartmentWithoutRoomsReturnsHttp201Created() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        DepartmentWithRoomsCreateDTO dto = new DepartmentWithRoomsCreateDTO("New Dept", b.getId(), null, null);
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Dept"))
                .andExpect(jsonPath("$.rooms").isEmpty());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatCreateNewDepartmentWithNewRoomsReturnsHttp201Created() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        DepartmentWithRoomsCreateDTO dto = new DepartmentWithRoomsCreateDTO("New Dept", b.getId(), null,
                List.of(new NewRoomInDepartmentDTO("Room-1", RoomType.OFFICE, 10)));
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Dept"))
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms[0]").exists());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatCreateNewDepartmentWithExistingRoomsReturnsHttp201Created() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Room room = TestDataUtil.createRoomEntity(null);
        room = roomRepository.save(room);
        DepartmentWithRoomsCreateDTO dto = new DepartmentWithRoomsCreateDTO("New Dept", b.getId(),
                List.of(room.getId()), null);
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Dept"))
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms[0]").exists());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatCreateNewDepartmentWithExistingRoomsAndNewRoomsReturnsHttp201Created() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Room room = TestDataUtil.createRoomEntity(null);
        room = roomRepository.save(room);
        DepartmentWithRoomsCreateDTO dto = new DepartmentWithRoomsCreateDTO("New Dept", b.getId(),
                List.of(room.getId()),
                List.of(new NewRoomInDepartmentDTO("Room-1", RoomType.OFFICE, 10)));
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Dept"))
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms[0]").exists())
                .andExpect(jsonPath("$.rooms[1]").exists());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatCreateNewDepartmentReturnsHttp409ConflictIfSameNameExists() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));
        DepartmentCreateDTO dto = new DepartmentCreateDTO(d.getName(), b.getId());
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatPatchSpecificDepartmentUpdatesFieldsSuccessfully() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department saved = departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));

        DepartmentCreateDTO patchDto = new DepartmentCreateDTO("Updated Dept", b.getId());
        String json = objectMapper.writeValueAsString(patchDto);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/departments/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Dept"));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatPatchSpecificDepartmentReturnsHttp409Conflict() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department saved = departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));
        Department second = TestDataUtil.createDepartmentEntity(b);
        second.setName("Test");
        departmentService.createDepartment(second);

        DepartmentCreateDTO patchDto = new DepartmentCreateDTO(second.getName(), b.getId());
        String json = objectMapper.writeValueAsString(patchDto);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/departments/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatDeleteDepartmentReturnsHttp204NoContent() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department saved = departmentService.createDepartment(TestDataUtil.createDepartmentEntity(b));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/departments/" + saved.getId() + "?onlyShared=false"))
                .andExpect(status().isNoContent());
        assertEquals(0, departmentRepository.findAll().size());
    }
}