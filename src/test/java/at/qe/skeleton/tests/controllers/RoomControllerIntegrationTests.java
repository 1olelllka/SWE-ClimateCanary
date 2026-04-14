package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.RoomCreateDTO;
import at.qe.skeleton.dtos.RoomPatchDTO;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.services.UserxService;
import at.qe.skeleton.tests.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RoomControllerIntegrationTests {

    private final MockMvc mockMvc;
    private final RoomService roomService;
    private final BuildingRepository buildingRepository;
    private final DepartmentRepository departmentRepository;
    private final RoomMonitoringRepository monitoringRepository;
    private final UserxService userxService;
    private final ObjectMapper objectMapper;

    @Autowired
    public RoomControllerIntegrationTests(RoomService roomService,
                                          BuildingRepository buildingRepository,
                                          DepartmentRepository departmentRepository,
                                          UserxService userxService,
                                          RoomMonitoringRepository monitoringRepository,
                                          MockMvc mockMvc) {
        this.roomService = roomService;
        this.buildingRepository = buildingRepository;
        this.departmentRepository = departmentRepository;
        this.mockMvc = mockMvc;
        this.userxService = userxService;
        this.monitoringRepository = monitoringRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Test
    public void testThatRoomsEndpointsAreSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    public void testThatGetPageOfRoomsReturnsHttp200OK() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        roomService.createRoom(TestDataUtil.createRoomEntity(d));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    public void testThatCreateNewRoomReturnsHttp409ConflictIfSuchNameExists() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        roomService.createRoom(Room.builder().roomNumber("Test").roomType(RoomType.OFFICE).department(d).defaultPeopleCnt(10).isActive(true).build());
        RoomCreateDTO dto = new RoomCreateDTO(d.getId(), RoomType.OFFICE, true, 5, "Test");
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
        assertEquals(1, monitoringRepository.findAll().size());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    public void testThatCreateNewRoomReturnsHttp201Created() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));

        RoomCreateDTO dto = new RoomCreateDTO(d.getId(), RoomType.OFFICE, true, 5, "Test");
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomType").value("OFFICE"))
                .andExpect(jsonPath("$.defaultPeopleCount").value(5));
        assertEquals(1, monitoringRepository.findAll().size());
    }

    @Test
    @WithMockUser(authorities = {"CAN_MANAGE_BUILDING_STRUCTURE", "CAN_MANAGE_USERS"})
    public void testThatPatchSpecificRoomUpdatesFieldsSuccessfully() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        Room saved = roomService.createRoom(TestDataUtil.createRoomEntity(d));

        // Update to SHARED and 20 people
        Userx newUser = userxService.saveUser(Userx.builder().username("Test").password("test").build());
        RoomPatchDTO patchDto = new RoomPatchDTO(d.getId(), RoomType.SHARED, true, 20, Set.of(newUser.getId()), "Test");
        String json = objectMapper.writeValueAsString(patchDto);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/rooms/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomType").value("SHARED"))
                .andExpect(jsonPath("$.defaultPeopleCount").value(20));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    public void testThatPatchSpecificRoomReturnsHttp404WhenNotFound() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));

        RoomCreateDTO dto = new RoomCreateDTO(d.getId(), RoomType.OFFICE, true, 5, "Test");
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/rooms/" + java.util.UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    public void testThatDeleteSpecificRoomReturnsHttp204NoContent() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        Room saved = roomService.createRoom(TestDataUtil.createRoomEntity(d));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/rooms/" + saved.getId()))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}