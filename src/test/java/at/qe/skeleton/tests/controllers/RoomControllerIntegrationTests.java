package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.RoomCreateDTO;
import at.qe.skeleton.dtos.RoomPatchDTO;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.model.RoomType;
import at.qe.skeleton.repositories.BuildingRepository;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.services.RoomService;
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RoomControllerIntegrationTests {

    private final MockMvc mockMvc;
    private final RoomService roomService;
    private final BuildingRepository buildingRepository;
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public RoomControllerIntegrationTests(RoomService roomService,
                                          BuildingRepository buildingRepository,
                                          DepartmentRepository departmentRepository,
                                          MockMvc mockMvc) {
        this.roomService = roomService;
        this.buildingRepository = buildingRepository;
        this.departmentRepository = departmentRepository;
        this.mockMvc = mockMvc;
        this.objectMapper = new ObjectMapper();
    }

    @Test
    public void testThatGetPageOfRoomsReturnsHttp200OK() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        roomService.createRoom(TestDataUtil.createRoomEntity(d));

        mockMvc.perform(MockMvcRequestBuilders.get("/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    public void testThatCreateNewRoomReturnsHttp201Created() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));

        RoomCreateDTO dto = new RoomCreateDTO(d.getId(), RoomType.OFFICE, true, 5);
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomType").value("OFFICE"))
                .andExpect(jsonPath("$.defaultPeopleCount").value(5));
    }

    @Test
    public void testThatPatchSpecificRoomUpdatesFieldsSuccessfully() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        Room saved = roomService.createRoom(TestDataUtil.createRoomEntity(d));

        // Update to SHARED and 20 people
        RoomPatchDTO patchDto = new RoomPatchDTO(d.getId(), RoomType.SHARED, true, 20);
        String json = objectMapper.writeValueAsString(patchDto);

        mockMvc.perform(MockMvcRequestBuilders.patch("/rooms/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomType").value("SHARED"))
                .andExpect(jsonPath("$.defaultPeopleCount").value(20));
    }

    @Test
    public void testThatPatchSpecificRoomReturnsHttp404WhenNotFound() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));

        RoomCreateDTO dto = new RoomCreateDTO(d.getId(), RoomType.OFFICE, true, 5);
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.patch("/rooms/" + java.util.UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testThatDeleteSpecificRoomReturnsHttp204NoContent() throws Exception {
        Building b = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department d = departmentRepository.save(TestDataUtil.createDepartmentEntity(b));
        Room saved = roomService.createRoom(TestDataUtil.createRoomEntity(d));

        mockMvc.perform(MockMvcRequestBuilders.delete("/rooms/" + saved.getId()))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(MockMvcRequestBuilders.get("/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}