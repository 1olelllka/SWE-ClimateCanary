package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.RaspberryCreateDTO;
import at.qe.skeleton.dtos.RaspberryPatchDTO;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.RoomOccupancy;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.RoomOccupancyRepository;
import at.qe.skeleton.services.RaspberryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
public class RaspberryControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RoomMonitoringRepository monitoringRepository;
    @Autowired
    private RaspberryPiRepository raspberryPiRepository;
    @Autowired
    private RaspberryService raspberryService;
    @MockitoBean
    private RoomOccupancyRepository occupancyRepository;

    private ObjectMapper objectMapper;
    private RoomMonitoring savedRoom;

    public RaspberryControllerIntegrationTests() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    void setUp() {
        monitoringRepository.deleteAll();
        raspberryPiRepository.deleteAll();

        this.savedRoom = monitoringRepository.save(RoomMonitoring.builder().roomId(UUID.randomUUID()).roomNumber("A101").build());
    }

    @Test
    public void testThatRaspberryPiEndpointsAreSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/raspberry-pis"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatGetPageOfRaspberryPisReturnsHttp200AndPage() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        raspberryService.createNewRaspberry(pi);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/raspberry-pis"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatGetSpecificRaspberryReturnsHttp404IfNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/raspberry-pis/" + UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatGetSpecificRaspberryReturnsHttp200OkIfSuccessful() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        pi = raspberryService.createNewRaspberry(pi);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/raspberry-pis/" + pi.getId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(pi.getId().toString()));
    }

    // /api/raspberry-pis/{raspberry_id}/sync/occupancy ... (after adding redis)
    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatSyncOccupancyReturnsHttp404IfSuchRaspberryDoesNotExist() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/raspberry-pis/" + UUID.randomUUID() + "/sync/occupancy"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatSyncOccupancyReturnsHttp200OkWithMockedRedis() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        pi = raspberryService.createNewRaspberry(pi);
        raspberryService.addNewRoom(pi.getId(), this.savedRoom.getRoomId());
        when(occupancyRepository.findAllById(List.of(this.savedRoom.getRoomId().toString()))).thenReturn(List.of(new RoomOccupancy(this.savedRoom.getRoomId(), 4)));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/raspberry-pis/" + pi.getId() + "/sync/occupancy"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].effectiveOccupancy").value("4"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].roomId").value(this.savedRoom.getRoomId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].privacyMode").value("true"));
    }

    // /api/raspberry-pis/{raspberry_id}/retry-connection ... (after compatibilities with devices)

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatRetryConnectionReturnsHttp404IfRaspberryNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/raspberry-pis/" + UUID.randomUUID() + "/retry-connection"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatRetryConnectionSavesDeadLettersAfterFailedRaspberryRetrial() throws Exception {
        RaspberryPi pi = raspberryService
                .createNewRaspberry(RaspberryPi.builder().ip("localhost").port(8000).name("Test raspberry").status(DeviceStatus.ONLINE).build());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/raspberry-pis/" + pi.getId() + "/retry-connection"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }


    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatCreateNewRaspberryReturnsHttp400IfValidationFails() throws Exception {
        RaspberryCreateDTO dto = new RaspberryCreateDTO("", null, null, null);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/raspberry-pis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatCreateNewRaspberryReturnsHttp409IfSuchNameExists() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        raspberryService.createNewRaspberry(pi);
        RaspberryCreateDTO dto = new RaspberryCreateDTO("Test Raspberry", "127.0.0.1", 1000, UUID.randomUUID());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/raspberry-pis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatCreateNewRaspberryReturnsHttp409IfSuchConnectionCredentialsExist() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        raspberryService.createNewRaspberry(pi);
        RaspberryCreateDTO dto = new RaspberryCreateDTO("Test Raspberry 2", "127.0.0.1", 1000, UUID.randomUUID());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/raspberry-pis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatCreateNewRaspberryReturnsHttp201IfSuccessful() throws Exception {
        RaspberryCreateDTO dto = new RaspberryCreateDTO("Test Raspberry", "127.0.0.1", 1000, this.savedRoom.getRoomId());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/raspberry-pis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(dto.name()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("OFFLINE"));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatPatchRaspberryReturnsHttp400IfValidationFails() throws Exception {
        RaspberryPatchDTO dto = new RaspberryPatchDTO("", null, null,  null);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/raspberry-pis/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatPatchRaspberryReturnsHttp404IfRaspberryNotExists() throws Exception {
        RaspberryPatchDTO dto = new RaspberryPatchDTO("Test Raspberry", "127.0.0.1", null, 1000);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/raspberry-pis/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatPatchRaspberryReturnsHttp409IfSuchNameExists() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        pi = raspberryService.createNewRaspberry(pi);
        raspberryService.createNewRaspberry(RaspberryPi.builder().name("Test Raspberry 2").ip("localhost").port(1000).status(DeviceStatus.OFFLINE).build());
        RaspberryCreateDTO dto = new RaspberryCreateDTO("Test Raspberry 2", "127.0.0.1", 1000, UUID.randomUUID());
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/raspberry-pis/" + pi.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatPatchRaspberryReturnsHttp200IfSuccessful() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        pi = raspberryService.createNewRaspberry(pi);
        RaspberryPatchDTO dto = new RaspberryPatchDTO("Updated Raspberry", null,20, 1000);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/raspberry-pis/" + pi.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(pi.getId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("OFFLINE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(dto.name()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ipAddress").value(pi.getIp()));
        assertEquals(dto.frequency(), raspberryPiRepository.findById(pi.getId()).get().getFrequency());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatDeleteSpecificRaspberryReturnsHttp204NoContentIfSuccessful() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        pi = raspberryService.createNewRaspberry(pi);
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/raspberry-pis/" + pi.getId()))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        assertFalse(raspberryPiRepository.existsById(pi.getId()));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatGetRaspberryPiConfigReturnsHttp404IfRaspberryNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/raspberry-pis/" + UUID.randomUUID() + "/config"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatGetRaspberryPiConfigReturnsHttp200OkIfSuccessful() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        pi = raspberryService.createNewRaspberry(pi);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/raspberry-pis/" + pi.getId() + "/config"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.frequency").value(100));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatAddNewRoomToRaspberryReturnsHttp404IfRaspberryNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/raspberry-pis/" + UUID.randomUUID() + "/rooms/" + UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatAddNewRoomToRaspberryReturnsHttp404IfRoomNotFound() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        pi = raspberryService.createNewRaspberry(pi);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/raspberry-pis/" + pi.getId() + "/rooms/" + UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatAddNewRoomToRaspberryReturnsHttp200OkIfSuccessful() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        pi = raspberryService.createNewRaspberry(pi);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/raspberry-pis/" + pi.getId() + "/rooms/" + this.savedRoom.getRoomId()))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatRemoveRoomFromRaspberryReturnsHttp404IfRaspberryNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/raspberry-pis/" + UUID.randomUUID() + "/rooms/" + UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatRemoveRoomFromRaspberryReturnsHttp404IfRoomNotFound() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        pi = raspberryService.createNewRaspberry(pi);
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/raspberry-pis/" + pi.getId() + "/rooms/" + UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatRemoveRoomFromRaspberryReturnsHttp200OkIfSuccessful() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        pi = raspberryService.createNewRaspberry(pi);
        raspberryService.addNewRoom(pi.getId(), this.savedRoom.getRoomId());
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/raspberry-pis/" + pi.getId() + "/rooms/" + this.savedRoom.getRoomId()))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatRemoveRoomFromRaspberryReturnsHttp404IfSuchRoomWasNotInsideOfRaspberry() throws Exception {
        RaspberryPi pi = RaspberryPi.builder().name("Test Raspberry").ip("127.0.0.1").port(1000).status(DeviceStatus.OFFLINE).build();
        pi = raspberryService.createNewRaspberry(pi);
        raspberryService.addNewRoom(pi.getId(), this.savedRoom.getRoomId());
        RaspberryPi pi2 = RaspberryPi.builder().name("Test Raspberry 2").ip("127.0.0.1").port(1001).status(DeviceStatus.OFFLINE).build();
        pi2 = raspberryService.createNewRaspberry(pi2);
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/raspberry-pis/" + pi2.getId() + "/rooms/" + this.savedRoom.getRoomId()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

}
