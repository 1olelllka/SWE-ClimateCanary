package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.*;
import at.qe.skeleton.feign.NotificationClient;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.services.UserxService;
import at.qe.skeleton.tests.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
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

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RoomControllerIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired RoomService roomService;
    @Autowired BuildingRepository buildingRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired RoomMonitoringRepository monitoringRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired UserxRepository userxRepository;
    @Autowired UserxService userxService;
    @Spy NotificationClient notificationClient;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userxRepository.deleteAll();
        monitoringRepository.deleteAll();
        roomRepository.deleteAll();
        departmentRepository.deleteAll();
        buildingRepository.deleteAll();
    }

    @Test
    void testThatRoomsEndpointsAreSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatGetPageOfRoomsReturnsHttp200OK() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        roomService.createRoom(TestDataUtil.createRoomEntity(d));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatCreateNewRoomReturnsHttp409ConflictIfSuchNameExistsWithinADepartment() throws Exception {
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
    void testThatCreateNewRoomReturnsHttp400BadRequestIfValidationFails() throws Exception {
        RoomCreateDTO dto = new RoomCreateDTO(UUID.randomUUID(), null, true, 5, "");
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }


    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatCreateNewRoomReturnsHttp201Created() throws Exception {
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
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatCreateNewRoomReturnsHttp201CreatedWithTheSameNameButOtherDepartment() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        roomService.createRoom(Room.builder().roomNumber("Test").roomType(RoomType.OFFICE).department(d).defaultPeopleCnt(10).isActive(true).build());
        Department d2 = TestDataUtil.createDepartmentEntity(b);
        d2.setName("Test 2");
        d2 = departmentRepository.save(d2);
        RoomCreateDTO dto = new RoomCreateDTO(d2.getId(), RoomType.OFFICE, true, 5, "Test");
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomType").value("OFFICE"))
                .andExpect(jsonPath("$.defaultPeopleCount").value(5));
        assertEquals(2, monitoringRepository.findAll().size());
    }

    @Test
    @WithMockUser(authorities = {"CAN_MANAGE_BUILDING_STRUCTURE", "CAN_MANAGE_USERS"})
    void testThatPatchSpecificRoomUpdatesFieldsSuccessfully() throws Exception {
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
    void testThatPatchSpecificRoomReturnsHttp404WhenNotFound() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));

        RoomPatchDTO dto = new RoomPatchDTO(d.getId(), RoomType.OFFICE, true, 5, null,"Test");
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/rooms/" + java.util.UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatPatchSpecificRoomReturnsHttp400IfValidationFails() throws Exception {
        RoomPatchDTO dto = new RoomPatchDTO(UUID.randomUUID(), null, true, 1, null, "");
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/rooms/" + java.util.UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_BUILDING_STRUCTURE")
    void testThatDeleteSpecificRoomReturnsHttp204NoContent() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        Room saved = roomService.createRoom(TestDataUtil.createRoomEntity(d));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/rooms/" + saved.getId()))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        verify(notificationClient, never()).notifyRaspberryAboutSensorChanges(any(URI.class), any(StateChangeNotificationDTO.class), any());
    }

    @Test
    @WithMockUser(authorities = "CAN_VIEW_ALL_ROOMS")
    void testThatGetAllLimitsForTheRoomReturnsHttp404NotFoundIfRoomNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/" + UUID.randomUUID() + "/limits"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_VIEW_ALL_ROOMS")
    void testThatGetAllLimitsForTHeRoomReturnsHttp200OkAndLimits() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        Room room = roomService.createRoom(TestDataUtil.createRoomEntity(d));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/" + room.getId() + "/limits"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.co2Max").value(70.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.tempMin").value(18.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.tempMax").value(26.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.humMin").value(30.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.humMax").value(60.0));
    }

    @Test
    @WithMockUser(authorities = "CAN_VIEW_ALL_ROOMS")
    void testThatPatchLimitsReturnsHttp404NotFoundIfRoomNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/rooms/" + UUID.randomUUID() + "/limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_VIEW_ALL_ROOMS")
    void testThatPatchLimitsReturnsHttp400BadRequestIfTemperatureMinimalLimitIsHigherThanMaximum() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        Room room = roomService.createRoom(TestDataUtil.createRoomEntity(d));
        LimitDTO dto = new LimitDTO(null, 100.0f, 50.0f, null, null, null);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/rooms/"+room.getId()+"/limits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_VIEW_ALL_ROOMS")
    void testThatPatchLimitsReturnsHttp400BadRequestIfHumidityMinimalLimitIsHigherThanMaximum() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        Room room = roomService.createRoom(TestDataUtil.createRoomEntity(d));
        LimitDTO dto = new LimitDTO(null, null, null, 100.0f, 50.0f, null);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/rooms/"+room.getId()+"/limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_VIEW_ALL_ROOMS")
    void testThatPatchLimitsReturnsHttp200OkAndUpdatedLimitsForARoom() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        Room room = roomService.createRoom(TestDataUtil.createRoomEntity(d));
        LimitDTO dto = new LimitDTO(null, null, null, null, null, 20.0f);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/rooms/"+room.getId()+"/limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.co2Max").value(20.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.tempMin").value(18.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.tempMax").value(26.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.humMin").value(30.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.humMax").value(60.0));
        verify(notificationClient, never()).notifyRaspberryAboutLimitsChange(any(URI.class), any(LimitChangeNotificationDTO.class));
    }
}