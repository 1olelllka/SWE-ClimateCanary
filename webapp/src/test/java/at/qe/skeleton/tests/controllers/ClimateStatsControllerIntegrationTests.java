package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.MeasurementBatchDTO;
import at.qe.skeleton.dtos.ReadingDTO;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.tests.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ClimateStatsControllerIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired
    ClimateStatsRepository climateRepository;
    @Autowired
    DepartmentRepository departmentRepository;
    @Autowired
    BuildingRepository buildingRepository;
    @Autowired
    RoomMonitoringRepository monitoringRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    RoomService roomService;
    @Autowired
    UserxRepository userxRepository;
    ObjectMapper mapper = new ObjectMapper();


    private RoomMonitoring roomMonitoring;
    private Userx employee;
    private Userx deptHead;
    private Userx building;

    @BeforeEach
    void setUp() {
        Building building = buildingRepository.save(TestDataUtil.createBuildingEntity());
        Department department = departmentRepository.save(TestDataUtil.createDepartmentEntity(building));
        Room room = roomService.createRoom(TestDataUtil.createRoomEntity(department));
        this.roomMonitoring = monitoringRepository.findById(room.getId()).get();
        this.employee = TestDataUtil.createUserxEntity(roleRepository.findAll().stream().filter(a -> a.getName().equals("EMPLOYEE")).findFirst().get(), room);
        this.employee.setUsername("employee");
        this.employee = userxRepository.save(employee);
        this.deptHead = TestDataUtil.createUserxEntity(roleRepository.findAll().stream().filter(a -> a.getName().equals("DEPARTMENT_MANAGER")).findFirst().get(), room);
        this.deptHead.setUsername("deptHead");
        this.deptHead = userxRepository.save(deptHead);
        this.building = TestDataUtil.createUserxEntity(roleRepository.findAll().stream().filter(a -> a.getName().equals("BUILDING_MANAGER")).findFirst().get(), null);
        this.building.setUsername("building");
        this.building = userxRepository.save(this.building);
    }

    @AfterEach
    void tearDown(){
        climateRepository.deleteAll();
        userxRepository.deleteAll();
        roomRepository.deleteAll();
        monitoringRepository.deleteAll();
        departmentRepository.deleteAll();
        buildingRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "CAN_SEND_MEASUREMENTS")
    void testThatPostMeasurementsReturnsHttp400BadRequestIfValidationFails() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/measurements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_SEND_MEASUREMENTS")
    void testThatPostMeasurementsReturnsHttp404NotFoundIfRoomWasNotFound() throws Exception {
        mapper.registerModule(new JavaTimeModule());
        MeasurementBatchDTO dto = new MeasurementBatchDTO(UUID.randomUUID(), OffsetDateTime.now(), List.of(new ReadingDTO(MeasurementType.TEMPERATURE, 10.0)));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/measurements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_SEND_MEASUREMENTS")
    void testThatPostMeasurementsReturnsHttp201CreatedIfSuccessful() throws Exception {
        mapper.registerModule(new JavaTimeModule());
        System.out.println(this.roomMonitoring);
        MeasurementBatchDTO dto = new MeasurementBatchDTO(this.roomMonitoring.getRoomId(), OffsetDateTime.now(), List.of(new ReadingDTO(MeasurementType.TEMPERATURE, 10.0)));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isCreated());
        assertEquals(1, climateRepository.findAll().size());
    }

    @Test
    void testThatGetCurrentClimateIsSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/current-climate".formatted(UUID.randomUUID().toString())))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void testThatGetCurrentClimateReturnsHttp404NotFoundIfRoomWasNotFound() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.building.getUsername(), this.building, this.building.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/current-climate".formatted(UUID.randomUUID().toString()))
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void testThatGetCurrentClimateForBuildingReturnsHttp404NotFoundIfNoRoomData() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.building.getUsername(), this.building, this.building.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/current-climate".formatted(this.roomMonitoring.getRoomId().toString()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void testThatGetCurrentClimateForBuildingReturnsHttp200OkAndData() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.building.getUsername(), this.building, this.building.getAuthorities());
        climateRepository.save(ClimateStats.builder().humVal(100).tempVal(120).pollVal(123).roomMonitoring(this.roomMonitoring).date(OffsetDateTime.now()).build());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/current-climate".formatted(this.roomMonitoring.getRoomId().toString()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.temperature").value(120));
    }

    @Test
    void testThatGetCurrentClimateForDepartmentManagerReturnsHttp403ForbiddenIfDifferentDepartment() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptHead.getUsername(), this.deptHead, this.deptHead.getAuthorities());
        Department department = TestDataUtil.createDepartmentEntity(buildingRepository.findAll().getFirst());
        department.setName("other dept");
        departmentRepository.save(department);
        Room room = TestDataUtil.createRoomEntity(department);
        room.setRoomNumber("other");
        room = roomRepository.save(room);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/current-climate".formatted(room.getId().toString()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void testThatGetCurrentClimateForDepartmentManagerReturnsHttp200OkAndData() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptHead.getUsername(), this.deptHead, this.deptHead.getAuthorities());
        climateRepository.save(ClimateStats.builder().humVal(100).tempVal(120).pollVal(123).roomMonitoring(this.roomMonitoring).date(OffsetDateTime.now()).build());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/current-climate".formatted(this.roomMonitoring.getRoomId().toString()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.temperature").value(120));
    }

    @Test
    void testThatGetCurrentClimateForEmployeeReturnsHttp403ForbiddenIfOtherOffice() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.employee.getUsername(), this.employee, this.employee.getAuthorities());
        Room room = TestDataUtil.createRoomEntity(departmentRepository.findAll().getFirst());
        room.setRoomNumber("other");
        room = roomRepository.save(room);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/current-climate".formatted(room.getId().toString()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void testThatGetCurrentClimateForEmployeeReturnsHttp403ForbiddenIfSharedInOtherDepartment() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.employee.getUsername(), this.employee, this.employee.getAuthorities());
        Department department = TestDataUtil.createDepartmentEntity(buildingRepository.findAll().getFirst());
        department.setName("other dept");
        departmentRepository.save(department);
        Room room = TestDataUtil.createRoomEntity(department);
        room.setRoomNumber("other");
        room.setRoomType(RoomType.SHARED);
        room = roomRepository.save(room);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/current-climate".formatted(room.getId().toString()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void testThatGetCurrentClimateForEmployeeReturnsHttp200OkAndData() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.employee.getUsername(), this.employee, this.employee.getAuthorities());
        climateRepository.save(ClimateStats.builder().humVal(100).tempVal(120).pollVal(123).roomMonitoring(this.roomMonitoring).date(OffsetDateTime.now()).build());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/current-climate".formatted(this.roomMonitoring.getRoomId().toString()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.temperature").value(120));
    }

    @Test
    void testThatGetOvertimeReturnsHttp400BadRequestIfInvalidTimestamps() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.employee.getUsername(), this.employee, this.employee.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/overtime?startDate=%s&endDate=%s".formatted(this.roomMonitoring.getRoomId().toString(), LocalDate.now().minusDays(5), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/overtime?startDate=%s&endDate=%s".formatted(this.roomMonitoring.getRoomId().toString(), LocalDate.now(), LocalDate.now().minusDays(1)))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void testThatGetOvertimeReturnsHttp404NotFoundIfNoRoomFound() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.building.getUsername(), this.building, this.building.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/overtime?startDate=%s&endDate=%s".formatted(UUID.randomUUID().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void testThatGetOvertimeForBuildingReturnsHttp200Ok() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.building.getUsername(), this.building, this.building.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/overtime?startDate=%s&endDate=%s".formatted(this.roomMonitoring.getRoomId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testThatGetOvertimeForDepartmentManagerAndEmployeeReturnsHttp403IfNotTheSameRoom() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptHead.getUsername(), this.deptHead, this.deptHead.getAuthorities());
        Room room = TestDataUtil.createRoomEntity(departmentRepository.findAll().getFirst());
        room.setRoomNumber("other");
        room = roomRepository.save(room);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/overtime?startDate=%s&endDate=%s".formatted(room.getId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        auth = new TestingAuthenticationToken(this.employee.getUsername(), this.employee, this.employee.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/overtime?startDate=%s&endDate=%s".formatted(room.getId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void testThatGetOvertimeForDepartmentManagerAndEmployeeReturnsHttp403IfNotTheSameSharedDepartment() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptHead.getUsername(), this.deptHead, this.deptHead.getAuthorities());
        Department department = TestDataUtil.createDepartmentEntity(buildingRepository.findAll().getFirst());
        department.setName("other dept");
        departmentRepository.save(department);
        Room room = TestDataUtil.createRoomEntity(department);
        room.setRoomNumber("other");
        room.setRoomType(RoomType.SHARED);
        room = roomRepository.save(room);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/overtime?startDate=%s&endDate=%s".formatted(room.getId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        auth = new TestingAuthenticationToken(this.employee.getUsername(), this.employee, this.employee.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/overtime?startDate=%s&endDate=%s".formatted(room.getId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void testThatGetOvertimeForDepartmentManagerAndEmployeeReturnsHttp200OkIfSuccessful() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptHead.getUsername(), this.deptHead, this.deptHead.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/overtime?startDate=%s&endDate=%s".formatted(this.roomMonitoring.getRoomId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());
        auth = new TestingAuthenticationToken(this.employee.getUsername(), this.employee, this.employee.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/overtime?startDate=%s&endDate=%s".formatted(this.roomMonitoring.getRoomId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testThatGetClimateHistoryReturnsHttp400BadRequestInBothCasesIfTimestampsAreInvalid() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.building.getUsername(), this.building, this.building.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s".formatted(this.roomMonitoring.getRoomId().toString(), LocalDate.now(), LocalDate.now().minusDays(1)))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        auth = new TestingAuthenticationToken(this.deptHead.getUsername(), this.deptHead, this.deptHead.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s".formatted(this.roomMonitoring.getRoomId().toString(), LocalDate.now(), LocalDate.now().minusDays(1)))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void testThatGetClimateHistoryForBuildingReturnsHttp404NotFoundIfRoomWasNotFound() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.building.getUsername(), this.building, this.building.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=HOUR".formatted(UUID.randomUUID().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void testThatGetClimateHistoryForBuildingReturnsHttp200OkInEveryGranularity() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.building.getUsername(), this.building, this.building.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=HOUR".formatted(this.roomMonitoring.getRoomId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=DAY".formatted(this.roomMonitoring.getRoomId().toString(), LocalDate.now().minusDays(10), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=WEEK".formatted(this.roomMonitoring.getRoomId().toString(), LocalDate.now().minusDays(100), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testThatGetClimateHistoryForEmployeeReturnsHttp403ForbiddenIfNotTheSameOffice() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.employee.getUsername(), this.employee, this.employee.getAuthorities());
        Room room = TestDataUtil.createRoomEntity(departmentRepository.findAll().getFirst());
        room.setRoomNumber("other");
        room = roomRepository.save(room);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=HOUR".formatted(room.getId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void testThatGetClimateHistoryForEmployeeReturnsHttp403ForbiddenIfNotTheSameDepartmentForShared() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.employee.getUsername(), this.employee, this.employee.getAuthorities());
        Department department = TestDataUtil.createDepartmentEntity(buildingRepository.findAll().getFirst());
        department.setName("other dept");
        departmentRepository.save(department);
        Room room = TestDataUtil.createRoomEntity(department);
        room.setRoomNumber("other");
        room.setRoomType(RoomType.SHARED);
        room = roomRepository.save(room);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=HOUR".formatted(room.getId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void testThatGetClimateHistoryForEmployeeReturnsHttp200OkWithDifferentGranularities() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.employee.getUsername(), this.employee, this.employee.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=HOUR".formatted(this.roomMonitoring.getRoomId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=DAY".formatted(this.roomMonitoring.getRoomId().toString(), LocalDate.now().minusDays(10), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=WEEK".formatted(this.roomMonitoring.getRoomId().toString(), LocalDate.now().minusDays(100), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testThatGetClimateHistoryForDepartmentManagerReturnsHttp404NotFoundIfRoomWasNotFound() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptHead.getUsername(), this.deptHead, this.deptHead.getAuthorities());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=HOUR".formatted(UUID.randomUUID().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void testThatGetClimateHistoryForDepartmentManagerReturnsHttp403ForbiddenIfNotTheSameDepartment() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptHead.getUsername(), this.deptHead, this.deptHead.getAuthorities());
        Department department = TestDataUtil.createDepartmentEntity(buildingRepository.findAll().getFirst());
        department.setName("other dept");
        departmentRepository.save(department);
        Room room = TestDataUtil.createRoomEntity(department);
        room.setRoomNumber("other");
        room.setRoomType(RoomType.SHARED);
        room = roomRepository.save(room);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=HOUR".formatted(room.getId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void testThatGetClimateHistoryForDepartmentManagerReturnsHttp403ForbiddenIfNotTheSameRoomAndHourly() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptHead.getUsername(), this.deptHead, this.deptHead.getAuthorities());
        Room room = TestDataUtil.createRoomEntity(departmentRepository.findAll().getFirst());
        room.setRoomNumber("other");
        room = roomRepository.save(room);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=HOUR".formatted(room.getId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void testThatGetClimateHistoryForDepartmentManagerReturnsHttp200OkIfNotTheSameRoomButShared() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptHead.getUsername(), this.deptHead, this.deptHead.getAuthorities());
        Room room = TestDataUtil.createRoomEntity(departmentRepository.findAll().getFirst());
        room.setRoomNumber("other");
        room.setRoomType(RoomType.SHARED);
        room = roomRepository.save(room);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=HOUR".formatted(room.getId().toString(), LocalDate.now().minusDays(1), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testThatGetClimateHistoryForDepartmentManagerReturnsHttp200OkIfNotTheSameRoomButDailyAndWeekly() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.deptHead.getUsername(), this.deptHead, this.deptHead.getAuthorities());
        Room room = TestDataUtil.createRoomEntity(departmentRepository.findAll().getFirst());
        room.setRoomNumber("other");
        room.setRoomType(RoomType.SHARED);
        room = roomRepository.save(room);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=DAY".formatted(room.getId().toString(), LocalDate.now().minusDays(10), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/rooms/%s/climate-history?startDate=%s&endDate=%s&granularity=WEEK".formatted(room.getId().toString(), LocalDate.now().minusDays(100), LocalDate.now()))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

}
