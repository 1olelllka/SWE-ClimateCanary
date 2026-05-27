package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.WarningCreateDTO;
import at.qe.skeleton.dtos.WarningDTO;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.services.WarningService;
import at.qe.skeleton.tests.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WarningControllerIntegrationTests {

    @Autowired
    WarningRepository warningRepository;
    @Autowired
    TipRepository tipRepository;
    @Autowired
    RoomService roomService;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    WarningService warningService;
    @Autowired
    UserxRepository userxRepository;
    @Autowired
    DepartmentRepository departmentRepository;
    @Autowired
    BuildingRepository buildingRepository;
    @Autowired
    RoomMonitoringRepository roomMonitoringRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    private RoomMonitoring room;
    private Userx employee;
    private Userx deptManager;
    private Userx senior;

    @BeforeEach
    void setUp() {
        warningRepository.deleteAll();
        tipRepository.deleteAll();
        userxRepository.deleteAll();
        roomRepository.deleteAll();
        departmentRepository.deleteAll();
        buildingRepository.deleteAll();
        roomMonitoringRepository.deleteAll();
        Building b = buildingRepository.save(Building.builder().name("Building").build());
        Department d = departmentRepository.save(Department.builder().name("Department").building(b).build());
        Room r = roomService.createRoom(TestDataUtil.createRoomEntity(d));
        this.room = roomMonitoringRepository.findById(r.getId()).get();

        employee = userxRepository.save(
                TestDataUtil.createUserxEntity(roleRepository.findAll().stream().filter(role -> role.getName().equals("EMPLOYEE")).findFirst().get(), r));
        deptManager = TestDataUtil.createUserxEntity(roleRepository.findAll().stream().filter(role -> role.getName().equals("DEPARTMENT_MANAGER")).findFirst().get(), r);
        deptManager.setUsername("deptManager");
        deptManager = userxRepository.save(deptManager);
        senior = TestDataUtil.createUserxEntity(roleRepository.findAll().stream().filter(role -> role.getName().equals("HIGHER_MANAGER")).findFirst().get(), null);
        senior.setUsername("senior");
        senior = userxRepository.save(senior);
    }

    @Test
    void testThatAllWarningsEndpointsAreSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/warnings"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void testThatGetWarningsForRoomForEmployeeReturnsOnlyCurrentWarnings() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.employee.getUsername(), this.employee, this.employee.getAuthorities());
        WarningDTO warning1 = warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.TEMPERATURE, WarningStatus.GREEN, 100, 41, "tip", UUID.randomUUID()));
        WarningDTO warning2 = warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.HUMIDITY, WarningStatus.RED, 100, 12, "tip", UUID.randomUUID()));
        warningService.resolveWarning(warning2.id());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/warnings/rooms/"+this.room.getRoomId()+"?activeOnly=true&startDate="+ LocalDate.now().minusDays(1)+"&endDate="+LocalDate.now())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(warning1.id().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").doesNotExist());
    }

    @Test
    void testThatGetWarningsForRoomForEmployeeReturnsHttp403ForbiddenIfItIsNotEmployeesRoom() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.employee.getUsername(), this.employee, this.employee.getAuthorities());
        Room room2 = TestDataUtil.createRoomEntity(departmentRepository.findAll().getFirst());
        room2.setRoomNumber("2ndRoom");
        room2.setRoomType(RoomType.OFFICE);
        room2 = roomService.createRoom(room2);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/warnings/rooms/"+room2.getId()+"?activeOnly=true&startDate="+ LocalDate.now().minusDays(1)+"&endDate="+LocalDate.now())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void testThatGetWarningsForDepartmentManagerReturnsHttp200OkAndResolvedAndActiveWarningsForRoom() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptManager.getUsername(), this.deptManager, this.deptManager.getAuthorities());
        warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.TEMPERATURE, WarningStatus.GREEN, 100, 41, "tip", UUID.randomUUID()));
        WarningDTO warning2 = warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.HUMIDITY, WarningStatus.RED, 100, 12, "tip", UUID.randomUUID()));
        warningService.resolveWarning(warning2.id());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/warnings/rooms/"+this.room.getRoomId()+"?activeOnly=false&startDate="+ LocalDate.now().minusDays(1)+"&endDate="+LocalDate.now())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").exists());
    }

    @Test
    void testThatGetWarningsForDepartmentManagerReturnsHttp200OkAndOnlyActiveWarningsForRoom() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptManager.getUsername(), this.deptManager, this.deptManager.getAuthorities());
        warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.TEMPERATURE, WarningStatus.GREEN, 100, 41, "tip", UUID.randomUUID()));
        WarningDTO warning2 = warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.HUMIDITY, WarningStatus.RED, 100, 12, "tip", UUID.randomUUID()));
        warningService.resolveWarning(warning2.id());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/warnings/rooms/"+this.room.getRoomId()+"?activeOnly=true&startDate="+ LocalDate.now().minusDays(1)+"&endDate="+LocalDate.now())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").doesNotExist());
    }

    @Test
    void testThatGetWarningsForDepartmentManagerReturnsHttp403ForbiddenIfDepartmentIsDifferent() throws Exception {
        Department d2 = TestDataUtil.createDepartmentEntity(buildingRepository.findAll().getFirst());
        d2.setName("Another department");
        d2 = departmentRepository.save(d2);
        Room room2 = TestDataUtil.createRoomEntity(d2);
        room2.setRoomNumber("2nd room");
        room2 = roomService.createRoom(room2);
        this.deptManager.setMyRoom(room2);
        userxRepository.save(this.deptManager);
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptManager.getUsername(), this.deptManager, this.deptManager.getAuthorities());
        warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.TEMPERATURE, WarningStatus.GREEN, 100, 41, "tip", UUID.randomUUID()));
        WarningDTO warning2 = warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.HUMIDITY, WarningStatus.RED, 100, 12, "tip", UUID.randomUUID()));
        warningService.resolveWarning(warning2.id());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/warnings/rooms/"+this.room.getRoomId()+"?activeOnly=true&startDate="+ LocalDate.now().minusDays(1)+"&endDate="+LocalDate.now())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CAN_SEND_WARNINGS")
    void testThatCreateNewWarningReturnsHttp404NotFoundIfRoomWasNotFound() throws Exception {
        WarningCreateDTO dto = new WarningCreateDTO(UUID.randomUUID(), MeasurementType.TEMPERATURE,
                WarningStatus.GREEN, 100, 43, "msg", UUID.randomUUID());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/warnings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_SEND_WARNINGS")
    void testThatCreateNewWarningReturnsHttp400BadRequestIfValidationFails() throws Exception {
        WarningCreateDTO dto = new WarningCreateDTO(UUID.randomUUID(), MeasurementType.TEMPERATURE,
                null, 100, 43, "msg", UUID.randomUUID());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/warnings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_SEND_WARNINGS")
    void testThatCreateNewWarningReturnsHttp201CreatedAndNoTipsForTemperature() throws Exception {
        WarningCreateDTO dto = new WarningCreateDTO(this.room.getRoomId(), MeasurementType.TEMPERATURE,
                WarningStatus.GREEN, 100, 43, "msg",  UUID.randomUUID());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/warnings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.tip").value("There's no tip."));
        assertNull(warningRepository.findAll().getFirst().getTip());
    }

    @Test
    @WithMockUser(authorities = "CAN_SEND_WARNINGS")
    void testThatCreateNewWarningReturnsHttp201CreatedAndNoTipsForHumidity() throws Exception {
        WarningCreateDTO dto = new WarningCreateDTO(this.room.getRoomId(), MeasurementType.HUMIDITY,
                WarningStatus.GREEN, 100, 43, "msg",  UUID.randomUUID());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/warnings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.tip").value("There's no tip."));
        assertNull(warningRepository.findAll().getFirst().getTip());
    }

    @Test
    @WithMockUser(authorities = "CAN_SEND_WARNINGS")
    void testThatCreateNewWarningReturnsHttp201CreatedAndNoTipsForAir() throws Exception {
        WarningCreateDTO dto = new WarningCreateDTO(this.room.getRoomId(), MeasurementType.CO2,
                WarningStatus.GREEN, 100, 43, "msg",  UUID.randomUUID());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/warnings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.tip").value("There's no tip."));
        assertNull(warningRepository.findAll().getFirst().getTip());
    }

    @Test
    @WithMockUser(authorities = "CAN_SEND_WARNINGS")
    void testThatCreateNewWarningReturnsHttp201CreatedAndTipsForTemperature() throws Exception {
        WarningCreateDTO dto = new WarningCreateDTO(this.room.getRoomId(), MeasurementType.TEMPERATURE,
                WarningStatus.GREEN, 100, 43, "msg",  UUID.randomUUID());
        tipRepository.save(Tip.builder().violationType(ViolationType.OVER).violationStatus(WarningStatus.GREEN).violatedSensor(ViolatedSensor.TEMPERATURE).msg("Open the window").build());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/warnings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.tip").value("Open the window"));
        assertNotNull(warningRepository.findAll().getFirst().getTip());
    }


    @Test
    @WithMockUser(authorities = "CAN_SEND_WARNINGS")
    void testThatCreateNewWarningReturnsHttp201CreatedAndTipsForHumidity() throws Exception {
        WarningCreateDTO dto = new WarningCreateDTO(this.room.getRoomId(), MeasurementType.HUMIDITY,
                WarningStatus.GREEN, 100, 43, "msg",  UUID.randomUUID());
        tipRepository.save(Tip.builder().violationType(ViolationType.OVER).violationStatus(WarningStatus.GREEN).violatedSensor(ViolatedSensor.HUMIDITY).msg("Open the window").build());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/warnings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.tip").value("Open the window"));
        assertNotNull(warningRepository.findAll().getFirst().getTip());
    }

    @Test
    @WithMockUser(authorities = "CAN_SEND_WARNINGS")
    void testThatCreateNewWarningReturnsHttp201CreatedAndTipsForAir() throws Exception {
        WarningCreateDTO dto = new WarningCreateDTO(this.room.getRoomId(), MeasurementType.CO2,
                WarningStatus.GREEN, 100, 43, "msg",  UUID.randomUUID());
        tipRepository.save(Tip.builder().violationType(ViolationType.OVER).violationStatus(WarningStatus.GREEN).violatedSensor(ViolatedSensor.AIR).msg("Open the window").build());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/warnings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.tip").value("Open the window"));
        assertNotNull(warningRepository.findAll().getFirst().getTip());
    }

    @Test
    @WithMockUser(authorities = "CAN_SEND_WARNINGS")
    void testThatResolveWarningReturnsHttp404NotFoundIfUnresolvedWarningWasNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/warnings/"+UUID.randomUUID()+"/resolve"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_SEND_WARNINGS")
    void testThatResolveWarningReturnsHttp403ForbiddenIfWarningIsAlreadyResolved() throws Exception {
        WarningDTO dto = warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.TEMPERATURE, WarningStatus.GREEN, 100, 41, "tip",  UUID.randomUUID()));
        warningService.resolveWarning(dto.id());
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/warnings/"+dto.id()+"/resolve"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CAN_SEND_WARNINGS")
    void testThatResolveWarningReturnsHttp200OkIfResolvedSuccessfully() throws Exception {
        WarningDTO dto = warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.TEMPERATURE, WarningStatus.GREEN, 100, 41, "tip",  UUID.randomUUID()));
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/warnings/"+dto.id()+"/resolve"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testThatGetDetailedViolationLogForDepartmentReturnsHttp404NotFoundIfDepartmentWasNotFound() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptManager.getUsername(), this.deptManager, this.deptManager.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/warnings/departments/"+UUID.randomUUID()+"/summary?onlyActive=false&startDate="+LocalDate.now().minusDays(1)+"&endDate="+LocalDate.now())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void testThatGetDetailedViolationLogForDepartmentReturnsHttp403ForbiddenIfDepartmentManagerHasOtherDepartment() throws Exception {
        UUID deptID = departmentRepository.findAll().getFirst().getId();
        Department d2 = TestDataUtil.createDepartmentEntity(buildingRepository.findAll().getFirst());
        d2.setName("Another department");
        d2 = departmentRepository.save(d2);
        Room room2 = TestDataUtil.createRoomEntity(d2);
        room2.setRoomNumber("2nd room");
        room2 = roomService.createRoom(room2);
        this.deptManager.setMyRoom(room2);
        userxRepository.save(this.deptManager);
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptManager.getUsername(), this.deptManager, this.deptManager.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/warnings/departments/"+deptID+"/summary?onlyActive=false&startDate="+LocalDate.now().minusDays(1)+"&endDate="+LocalDate.now())
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void testThatGetDetailedViolationLogForDepartmentReturnsHttp200OkAndAllActiveWarningsFromDifferentRoomsWithinDepartment() throws Exception {
        UUID deptID = departmentRepository.findAll().getFirst().getId();
        warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.TEMPERATURE, WarningStatus.GREEN, 100, 41, "tip",  UUID.randomUUID()));
        WarningDTO warning2 = warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.HUMIDITY, WarningStatus.RED, 100, 12, "tip",  UUID.randomUUID()));
        warningService.resolveWarning(warning2.id());
        Room room2 = TestDataUtil.createRoomEntity(departmentRepository.findAll().getFirst());
        room2.setRoomNumber("Another room");
        room2 = roomService.createRoom(room2);
        warningService.createWarning(new WarningCreateDTO(room2.getId(),
                MeasurementType.HUMIDITY, WarningStatus.RED, 100, 12, "tip", UUID.randomUUID()));
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptManager.getUsername(), this.deptManager, this.deptManager.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/warnings/departments/"+deptID+"/summary?onlyActive=true&startDate="+LocalDate.now().minusDays(1)+"&endDate="+LocalDate.now())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].roomId").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].roomId").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].id").doesNotExist());
    }

    @Test
    void testThatGetDetailedViolationLogForDepartmentReturnsHttp200OkAndAllWarningsFromDifferentRoomsWithinDepartment() throws Exception {
        UUID deptID = departmentRepository.findAll().getFirst().getId();
        warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.TEMPERATURE, WarningStatus.GREEN, 100, 41, "tip", UUID.randomUUID()));
        WarningDTO warning2 = warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.HUMIDITY, WarningStatus.RED, 100, 12, "tip", UUID.randomUUID()));
        warningService.resolveWarning(warning2.id());
        Room room2 = TestDataUtil.createRoomEntity(departmentRepository.findAll().getFirst());
        room2.setRoomNumber("Another room");
        room2 = roomService.createRoom(room2);
        warningService.createWarning(new WarningCreateDTO(room2.getId(),
                MeasurementType.HUMIDITY, WarningStatus.RED, 100, 12, "tip", UUID.randomUUID()));
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptManager.getUsername(), this.deptManager, this.deptManager.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/warnings/departments/"+deptID+"/summary?onlyActive=false&startDate="+LocalDate.now().minusDays(1)+"&endDate="+LocalDate.now())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].roomId").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].roomId").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].roomId").exists());
    }

    @Test
    void testThatGetViolationLogForHigherManagementReturnsHttp404NotFoundIfDepartmentWasNotFound() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.senior.getUsername(), this.senior, this.senior.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/warnings/departments/"+UUID.randomUUID()+"/summary?onlyActive=false&startDate="+LocalDate.now().minusDays(1)+"&endDate="+LocalDate.now())
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void testThatGetViolationLogForHigherManagementReturnsHttp200OkAndAllActiveWarningsFromDifferentRoomsWithinDepartmentWithAnonymousRooms() throws Exception {
        UUID deptID = departmentRepository.findAll().getFirst().getId();
        warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.TEMPERATURE, WarningStatus.GREEN, 100, 41, "tip", UUID.randomUUID()));
        WarningDTO warning2 = warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.HUMIDITY, WarningStatus.RED, 100, 12, "tip", UUID.randomUUID()));
        warningService.resolveWarning(warning2.id());
        Room room2 = TestDataUtil.createRoomEntity(departmentRepository.findAll().getFirst());
        room2.setRoomNumber("Another room");
        room2 = roomService.createRoom(room2);
        warningService.createWarning(new WarningCreateDTO(room2.getId(),
                MeasurementType.HUMIDITY, WarningStatus.RED, 100, 12, "tip", UUID.randomUUID()));
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.senior.getUsername(), this.senior, this.senior.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/warnings/departments/"+deptID+"/summary?onlyActive=true&startDate="+LocalDate.now().minusDays(1)+"&endDate="+LocalDate.now())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].roomId").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].roomId").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].id").doesNotExist());
    }

    @Test
    void testThatGetViolationLogForHigherManagementReturnsHttp200OkAndAllWarningsFromDifferentRoomsWithinDepartmentWithAnonymousRooms() throws Exception {
        UUID deptID = departmentRepository.findAll().getFirst().getId();
        warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.TEMPERATURE, WarningStatus.GREEN, 100, 41, "tip", UUID.randomUUID()));
        WarningDTO warning2 = warningService.createWarning(new WarningCreateDTO(this.room.getRoomId(),
                MeasurementType.HUMIDITY, WarningStatus.RED, 100, 12, "tip", UUID.randomUUID()));
        warningService.resolveWarning(warning2.id());
        Room room2 = TestDataUtil.createRoomEntity(departmentRepository.findAll().getFirst());
        room2.setRoomNumber("Another room");
        room2 = roomService.createRoom(room2);
        warningService.createWarning(new WarningCreateDTO(room2.getId(),
                MeasurementType.HUMIDITY, WarningStatus.RED, 100, 12, "tip", UUID.randomUUID()));
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.senior.getUsername(), this.senior, this.senior.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/warnings/departments/"+deptID+"/summary?onlyActive=false&startDate="+LocalDate.now().minusDays(1)+"&endDate="+LocalDate.now())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].roomId").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].roomId").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].roomId").doesNotExist());
    }

}
