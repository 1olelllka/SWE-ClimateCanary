package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.AbsenceCreateDTO;
import at.qe.skeleton.dtos.AbsencePatchDTO;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.*;
import at.qe.skeleton.tests.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AbsenceControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private AbsenceService absenceService;
    @Autowired private UserService userService;
    @Autowired private UserRoleService userRoleService;
    @Autowired private RoomService roomService;
    @Autowired private DepartmentService departmentService;
    @Autowired private BuildingService buildingService;
    @Autowired private UserxRepository userxRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private AbsenceRepository absenceRepository;
    @Autowired private RoomMonitoringRepository monitoringRepository;
    @MockitoBean
    private UserClockStatusRepository clockRepository;
    @MockitoBean
    private RoomOccupancyRepository roomOccupancyRepository;
    @Autowired private ObjectMapper objectMapper;

    private Absence mockedAbsence;
    private Userx user;
    private Userx manager;
    private Building building;

    @BeforeEach
    void setUp() {
        List<UserRole> userRoles = userRoleService.getListOfPermissions();
        this.building = buildingService.createBuilding(TestDataUtil.createBuildingEntity());
        Department department = departmentService.createDepartment(TestDataUtil.createDepartmentEntity(this.building));
        Room room = roomService.createRoom(TestDataUtil.createRoomEntity(department));
        this.user = TestDataUtil.createUserxEntity(userRoles.stream().filter(u -> u.getName().equals("EMPLOYEE")).toList().getFirst(), null);
        this.user.setPassword("passwd");
        this.user.setMyRoom(room);
        this.user = userService.createNewUser(user);
        this.manager = TestDataUtil.createUserxEntity(userRoles.stream().filter(u -> u.getName().equals("DEPARTMENT_MANAGER")).toList().getFirst(), null);
        this.manager.setUsername("manager");
        this.manager.setPassword("passwd");
        this.manager.setMyRoom(room);
        this.manager = userService.createNewUser(this.manager);
        this.mockedAbsence = TestDataUtil.createAbsence(user);
        this.mockedAbsence.setAssignedTo(this.manager.getId());
        this.mockedAbsence = absenceService.createNewAbsenceForUser(this.mockedAbsence);
    }

    @AfterEach
    void tearDown() {
        monitoringRepository.deleteAll();
        absenceRepository.deleteAll();
        userxRepository.deleteAll();
        roomRepository.deleteAll();
        departmentRepository.deleteAll();
        buildingRepository.deleteAll();
        clockRepository.deleteAll();
        roomOccupancyRepository.deleteAll();
    }

    @Test
    void testThatAbsenceEndpointsAreSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/absences"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void testThatGetAllAbsencesForEmployeeReturnsListOfUsersAbsences() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.user.getUsername(), this.user, "CAN_MANAGE_OWN_ABSENCE");
        mockMvc.perform(MockMvcRequestBuilders.get("/api/absences")
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id").value(mockedAbsence.getId().toString()));
    }

    @Test
    void testThatGetAllAbsencesForDepartmentManagerReturnsListOfAbsencesForDepartment() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.manager.getUsername(), this.manager, "CAN_VIEW_ABSENCE_VIEW");
        mockMvc.perform(MockMvcRequestBuilders.get("/api/absences")
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id").value(mockedAbsence.getId().toString()));
    }

    @Test
    @WithMockUser(roles = "BUILDING_MANAGER")
    void testThatCreateAbsenceIsSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_OWN_ABSENCE")
    void testThatCreateAbsenceReturnsHttp400IfValidationFails() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_OWN_ABSENCE")
    void testThatCreateAbsenceReturnsHttp400IfStartDateIsAfterEndDate() throws Exception {
        AbsenceCreateDTO dto = new AbsenceCreateDTO(this.user.getId(),
                LocalDateTime.of(2028, 2, 2, 0, 0),
                LocalDateTime.of(2026, 2, 2, 0, 0),
                AbsenceType.ILLNESS,
                "Comment",
                this.manager.getId());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_OWN_ABSENCE")
    void testThatCreateAbsenceReturnsHttp404IfManagerWasNotFound() throws Exception {
        AbsenceCreateDTO dto = new AbsenceCreateDTO(this.user.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5),
                AbsenceType.ILLNESS,
                "Comment",
                UUID.randomUUID());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_OWN_ABSENCE")
    void testThatCreateAbsenceReturnsHttp403IfManagerRoleIsIncorrect() throws Exception {
        Userx mockManager = TestDataUtil.createUserxEntity(userRoleService.getListOfPermissions().stream().filter(r -> r.getName().equals("EMPLOYEE")).toList().getFirst(), null);
        mockManager.setUsername("mockManager");
        mockManager.setPassword("passwd");
        mockManager = userService.createNewUser(mockManager);
        AbsenceCreateDTO dto = new AbsenceCreateDTO(this.user.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5),
                AbsenceType.ILLNESS,
                "Comment",
                mockManager.getId());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("Assigned person does not have manager rights."));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_OWN_ABSENCE")
    void testThatCreateAbsenceReturnsHttp403IfDepartmentOfManagerIsIncorrect() throws Exception {
        Department dep = TestDataUtil.createDepartmentEntity(this.building);
        dep.setName("Another department");
        dep = departmentService.createDepartment(dep);
        Room newRoom = TestDataUtil.createRoomEntity(dep);
        newRoom.setRoomNumber("Test 2");
        newRoom = roomService.createRoom(newRoom);
        roomService.patchRoom(newRoom.getId(), Room.builder()
                .users(Set.of(this.manager)).build());
        AbsenceCreateDTO dto = new AbsenceCreateDTO(this.user.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5),
                AbsenceType.ILLNESS,
                "Comment",
                this.manager.getId());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("You cannot apply for absence to this manager."));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_OWN_ABSENCE")
    void testThatCreateAbsenceReturnsHttp400BadRequestIfNumberOfDaysIsTooBig() throws Exception {
        AbsenceCreateDTO dto = new AbsenceCreateDTO(this.user.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(30),
                AbsenceType.ILLNESS,
                "Comment",
                this.manager.getId());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_OWN_ABSENCE")
    void testThatCreateAbsenceReturnsHttp201AndCreatedAbsence() throws Exception {
        AbsenceCreateDTO dto = new AbsenceCreateDTO(this.user.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5),
                AbsenceType.ILLNESS,
                "Comment",
                this.manager.getId());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists());
        assertEquals(12, userxRepository.findById(this.user.getId()).get().getNumberOfAbsences());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_ABSENCES")
    void testThatPatchAbsenceReturnsHttp400IfValidationFails() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/absences/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString("{}")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_ABSENCES")
    void testThatPatchAbsenceReturnsHttp404IfAbsenceWasNotFound() throws Exception {
        AbsencePatchDTO dto = new AbsencePatchDTO(AbsenceStatus.APPROVED);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/absences/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_ABSENCES")
    void testThatPatchAbsenceReturnsHttp200OkAndUpdatedAbsence() throws Exception {
        AbsencePatchDTO dto = new AbsencePatchDTO(AbsenceStatus.APPROVED);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/absences/" + this.mockedAbsence.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(this.mockedAbsence.getId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(dto.status().name()));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_ABSENCES")
    void testThatPatchAbsenceReturnsHttp200OkAndUpdatedAbsenceWithNewAbsenceDaysForUser() throws Exception {
        AbsencePatchDTO dto = new AbsencePatchDTO(AbsenceStatus.REJECTED);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/absences/" + this.mockedAbsence.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(this.mockedAbsence.getId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(dto.status().name()));
        assertEquals(25, userxRepository.findById(this.user.getId()).get().getNumberOfAbsences());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_ABSENCES")
    void testThatGetSpecificAbsenceReturnsHttp404IfAbsenceWasNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/absences/" + UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void testThatGetSpecificAbsenceReturnsHttp403IfAssignedIDIsOther() throws Exception {
        Userx mockManager = TestDataUtil.createUserxEntity(userRoleService.getListOfPermissions().stream().filter(r -> r.getName().equals("EMPLOYEE")).toList().getFirst(), null);
        mockManager.setUsername("mockManager");
        mockManager.setPassword("passwd");
        mockManager = userService.createNewUser(mockManager);
        TestingAuthenticationToken auth = new TestingAuthenticationToken(mockManager.getUsername(), mockManager, "CAN_MANAGE_ABSENCES");
        mockMvc.perform(MockMvcRequestBuilders.get("/api/absences/" + this.mockedAbsence.getId())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void testThatGetSpecificAbsenceReturnsHttp200Ok() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.manager.getUsername(), this.manager, "CAN_MANAGE_ABSENCES");
        mockMvc.perform(MockMvcRequestBuilders.get("/api/absences/" + this.mockedAbsence.getId())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(this.mockedAbsence.getId().toString()));
    }

    @Test
    void testThatClockInReturnsHttp409ConflictIfUserHasAlreadyClockedIn() throws Exception {

        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.user.getUsername(), this.user, "CAN_MANAGE_OWN_ABSENCE");
        when(clockRepository.findById(this.user.getId().toString())).thenReturn(Optional.of(new UserClockStatus(this.user.getId(), true)));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences/clock-in").with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    void testThatClockInReturnsHttp200OkIfUserDoesNotHaveRoomAndTheStatusIsUpdated() throws Exception {
        Userx sampleUser = TestDataUtil.createUserxEntity(userRoleService.getListOfPermissions().stream().filter(u -> u.getName().equals("EMPLOYEE")).toList().getFirst(), null);
        sampleUser.setUsername("sample username");
        sampleUser.setPassword("passwd");
        sampleUser = userService.createNewUser(sampleUser);
        TestingAuthenticationToken auth = new TestingAuthenticationToken(sampleUser.getUsername(), sampleUser, "CAN_MANAGE_OWN_ABSENCE");
        when(clockRepository.findById(this.user.getId().toString())).thenReturn(Optional.of(new UserClockStatus(this.user.getId(), false)));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences/clock-in").with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());
        verify(clockRepository, times(1)).save(any(UserClockStatus.class));
    }

    @Test
    void testThatClockInReturnsHttp200OkOnSuccessfulClockInIfItIsNotInRedis() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.user.getUsername(), this.user, "CAN_MANAGE_OWN_ABSENCE");
        when(clockRepository.findById(this.user.getId().toString())).thenReturn(Optional.of(new UserClockStatus(this.user.getId(), false)));
        when(roomOccupancyRepository.findById(this.user.getMyRoom().getId().toString())).thenReturn(Optional.empty());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences/clock-in").with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());
        verify(roomOccupancyRepository, times(1)).save(any(RoomOccupancy.class));
        verify(clockRepository, times(1)).save(any(UserClockStatus.class));
    }

    @Test
    void testThatClockInReturnsHttp200OkOnSuccessfulClockInIfItIsInRedis() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.user.getUsername(), this.user, "CAN_MANAGE_OWN_ABSENCE");
        when(clockRepository.findById(this.user.getId().toString())).thenReturn(Optional.of(new UserClockStatus(this.user.getId(), false)));
        when(roomOccupancyRepository.findById(this.user.getMyRoom().getId().toString())).thenReturn(Optional.of(new RoomOccupancy(this.user.getMyRoom().getId(), 10)));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences/clock-in").with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());
        verify(roomOccupancyRepository, times(1)).save(any(RoomOccupancy.class));
        verify(clockRepository, times(1)).save(any(UserClockStatus.class));
    }

    @Test
    void testThatClockOutEndpointIsSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences/clock-out"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void testThatClockOutReturnsHttp409ConflictIfUserNeverClockedIn() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                this.user.getUsername(), this.user, "CAN_MANAGE_OWN_ABSENCE");
        when(clockRepository.findById(this.user.getId().toString())).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/absences/clock-out")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    void testThatClockOutReturnsHttp409ConflictIfUserAlreadyClockedOut() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                this.user.getUsername(), this.user, "CAN_MANAGE_OWN_ABSENCE");
        when(clockRepository.findById(this.user.getId().toString()))
                .thenReturn(Optional.of(new UserClockStatus(this.user.getId(), false)));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/absences/clock-out")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    void testThatClockOutReturnsHttp200OkIfUserHasNoRoom() throws Exception {
        Userx userWithoutRoom = TestDataUtil.createUserxEntity(
                userRoleService.getListOfPermissions().stream().filter(u -> u.getName().equals("EMPLOYEE")).toList().getFirst(), null);
        userWithoutRoom.setUsername("clockout-noroomuser");
        userWithoutRoom.setPassword("passwd");
        userWithoutRoom = userService.createNewUser(userWithoutRoom);
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                userWithoutRoom.getUsername(), userWithoutRoom, "CAN_MANAGE_OWN_ABSENCE");
        when(clockRepository.findById(userWithoutRoom.getId().toString()))
                .thenReturn(Optional.of(new UserClockStatus(userWithoutRoom.getId(), true)));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/absences/clock-out")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(clockRepository, times(1)).save(any(UserClockStatus.class));
        verifyNoInteractions(roomOccupancyRepository);
    }

    @Test
    void testThatClockOutReturnsHttp200OkAndDecrementsOccupancyWhenRecordExists() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                this.user.getUsername(), this.user, "CAN_MANAGE_OWN_ABSENCE");
        when(clockRepository.findById(this.user.getId().toString()))
                .thenReturn(Optional.of(new UserClockStatus(this.user.getId(), true)));
        when(roomOccupancyRepository.findById(this.user.getMyRoom().getId().toString()))
                .thenReturn(Optional.of(new RoomOccupancy(this.user.getMyRoom().getId(), 5)));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/absences/clock-out")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(roomOccupancyRepository, times(1)).save(any(RoomOccupancy.class));
        verify(clockRepository, times(1)).save(any(UserClockStatus.class));
    }

    @Test
    void testThatClockOutReturnsHttp200OkAndCreatesOccupancyRecordWhenNoneExists() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                this.user.getUsername(), this.user, "CAN_MANAGE_OWN_ABSENCE");
        when(clockRepository.findById(this.user.getId().toString()))
                .thenReturn(Optional.of(new UserClockStatus(this.user.getId(), true)));
        when(roomOccupancyRepository.findById(this.user.getMyRoom().getId().toString()))
                .thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/absences/clock-out")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(roomOccupancyRepository, times(1)).save(any(RoomOccupancy.class));
        verify(clockRepository, times(1)).save(any(UserClockStatus.class));
    }

    @Test
    void testThatClockOutDoesNotDecrementOccupancyBelowZero() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                this.user.getUsername(), this.user, "CAN_MANAGE_OWN_ABSENCE");
        when(clockRepository.findById(this.user.getId().toString()))
                .thenReturn(Optional.of(new UserClockStatus(this.user.getId(), true)));
        when(roomOccupancyRepository.findById(this.user.getMyRoom().getId().toString()))
                .thenReturn(Optional.of(new RoomOccupancy(this.user.getMyRoom().getId(), 0)));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/absences/clock-out")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(roomOccupancyRepository, times(1)).save(any(RoomOccupancy.class));
        verify(clockRepository, times(1)).save(any(UserClockStatus.class));
    }

    @Test
    void testThatGetClockStatusReturnsHttp200OkAndTrueClockStatus() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                this.user.getUsername(), this.user, "ROLE_EMPLOYEE"
        );
        when(clockRepository.findById(this.user.getId().toString())).thenReturn(Optional.of(new UserClockStatus(this.user.getId(), true)));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/absences/clock-status")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.clockedIn").value("true"));
    }

    @Test
    void testThatGetClockStatusReturnsHttp200OkAndFalseClockStatus() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                this.user.getUsername(), this.user, "ROLE_EMPLOYEE"
        );
        when(clockRepository.findById(this.user.getId().toString())).thenReturn(Optional.empty());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/absences/clock-status")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.clockedIn").value("false"));
    }

}
