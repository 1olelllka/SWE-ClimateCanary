package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.SensorStationCreateDTO;
import at.qe.skeleton.dtos.SensorStationPatchDTO;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.model.RoomMonitoring;
import at.qe.skeleton.model.SensorStation;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.repositories.RoomMonitoringRepository;
import at.qe.skeleton.repositories.SensorStationRepository;
import at.qe.skeleton.services.RaspberryService;
import at.qe.skeleton.services.SensorStationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
public class SensorStationControllerIntegrationTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SensorStationService sensorService;
    @Autowired
    private RaspberryService raspberryService;
    @Autowired
    private RaspberryPiRepository raspberryPiRepository;
    @Autowired
    private SensorStationRepository sensorRepository;
    @Autowired
    private RoomMonitoringRepository monitoringRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private RoomMonitoring savedRoom;

    @BeforeEach
    void setUp() {
        monitoringRepository.deleteAll();
        sensorRepository.deleteAll();
        raspberryPiRepository.deleteAll();
        this.savedRoom = monitoringRepository.save(RoomMonitoring.builder().roomId(UUID.randomUUID()).roomNumber("A101").build());
        RaspberryPi pi = RaspberryPi.builder().ip("localhost").port(8000).name("Test Raspberry").status(DeviceStatus.ONLINE).build();
        raspberryService.createNewRaspberry(pi);
    }


    @Test
    public void testThatSensorStationsEndpointsAreSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/sensor-stations"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatGetAllSensorStationsReturnsHttp200AndPage() throws Exception {
        SensorStation station = SensorStation.builder().name("Hallway-Sensor").status(DeviceStatus.OFFLINE).roomMonitoring(this.savedRoom).build();
        sensorService.createNewSensorStation(station);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/sensor-stations"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].name").value("Hallway-Sensor"));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatCreateNewSensorReturnsHttp400IfValidationFails() throws Exception {
        SensorStationCreateDTO dto = new SensorStationCreateDTO("Hallway-Sensor", null);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/sensor-stations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatCreateNewSensorReturnsHttp404IfRoomNotFound() throws Exception {
        SensorStationCreateDTO dto = new SensorStationCreateDTO("Hallway-Sensor", UUID.randomUUID());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/sensor-stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatCreateNewSensorReturnsHttp409IfNameAlreadyExist() throws Exception {
        SensorStation station = SensorStation.builder().name("Hallway-Sensor").status(DeviceStatus.OFFLINE).roomMonitoring(this.savedRoom).build();
        sensorService.createNewSensorStation(station);
        SensorStationCreateDTO dto = new SensorStationCreateDTO("Hallway-Sensor", savedRoom.getRoomId());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/sensor-stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatCreateNewSensorReturnsHttp201IfSuccessful() throws Exception {
        SensorStationCreateDTO dto = new SensorStationCreateDTO("Hallway-Sensor", savedRoom.getRoomId());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/sensor-stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Hallway-Sensor"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("OFFLINE"));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatPatchSensorReturnsHttp400IfValidationFails() throws Exception {
        SensorStationPatchDTO dto = new SensorStationPatchDTO("", null, null, null);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/sensor-stations/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatPatchSensorReturnsHttp404IfSensorNotFound() throws Exception {
        SensorStationPatchDTO dto = new SensorStationPatchDTO("Hallway-Sensor", null, null, null);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/sensor-stations/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatPatchSensorReturnsHttp404IfRoomNotFound() throws Exception {
        SensorStation station = SensorStation.builder().name("Hallway-Sensor").status(DeviceStatus.OFFLINE).roomMonitoring(this.savedRoom).build();
        station = sensorService.createNewSensorStation(station);
        SensorStationPatchDTO dto = new SensorStationPatchDTO("Hallway-Sensor", null, null, UUID.randomUUID());
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/sensor-stations/" + station.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatPatchSensorReturnsHttp409IfNameAlreadyExist() throws Exception {
        SensorStation station = SensorStation.builder().name("Hallway-Sensor").status(DeviceStatus.OFFLINE).roomMonitoring(this.savedRoom).build();
        station = sensorService.createNewSensorStation(station);
        RoomMonitoring room2 = monitoringRepository.save(RoomMonitoring.builder().roomId(UUID.randomUUID()).roomNumber("A102").build());
        sensorService.createNewSensorStation(SensorStation.builder().name("Hallway-Sensor 2").status(DeviceStatus.OFFLINE).roomMonitoring(room2).build()
        );
        SensorStationPatchDTO dto = new SensorStationPatchDTO("Hallway-Sensor 2", null, null, savedRoom.getRoomId());
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/sensor-stations/" + station.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatPatchSensorReturnsHttp201IfSuccessful() throws Exception {
        SensorStation station = SensorStation.builder().name("Hallway-Sensor").status(DeviceStatus.OFFLINE).roomMonitoring(this.savedRoom).build();
        station = sensorService.createNewSensorStation(station);
        SensorStationPatchDTO dto = new SensorStationPatchDTO("Hallway-Sensor 2", null, DeviceStatus.ONLINE, null);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/sensor-stations/" + station.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Hallway-Sensor 2"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ONLINE"));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatGetSpecificStationReturnsHttp404IfNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/sensor-stations/" + UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatGetSpecificStationReturnsHttp200IfSuccessful() throws Exception {
        SensorStation station = SensorStation.builder().name("Hallway-Sensor").status(DeviceStatus.OFFLINE).roomMonitoring(this.savedRoom).build();
        station = sensorService.createNewSensorStation(station);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/sensor-stations/" + station.getId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(station.getId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("OFFLINE"));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatDeleteSensorReturnsHttp404IfSensorNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/sensor-stations/" + UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_DEVICES")
    public void testThatDeleteSensorReturnsHttp204IfSuccessful() throws Exception {
        SensorStation station = SensorStation.builder().name("Hallway-Sensor").status(DeviceStatus.OFFLINE).roomMonitoring(this.savedRoom).build();
        station = sensorService.createNewSensorStation(station);
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/sensor-stations/" + station.getId()))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

}
