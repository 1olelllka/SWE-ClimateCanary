package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.AbsenceCreateDTO;
import at.qe.skeleton.dtos.AbsencePatchDTO;
import at.qe.skeleton.model.*;
import at.qe.skeleton.services.*;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AbsenceControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private AbsenceService absenceService;
    @Autowired private UserService userService;
    @Autowired private UserRoleService userRoleService;
    @Autowired private RoomService roomService;
    @Autowired private DepartmentService departmentService;
    @Autowired private BuildingService buildingService;
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

    @Test
    public void testThatAbsenceEndpointsAreSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/absences"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void testThatGetAllAbsencesForEmployeeReturnsListOfUsersAbsences() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.user.getUsername(), this.user, "ROLE_EMPLOYEE");
        mockMvc.perform(MockMvcRequestBuilders.get("/api/absences")
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id").value(mockedAbsence.getId().toString()));
    }

    @Test
    public void testThatGetAllAbsencesForDepartmentManagerReturnsListOfAbsencesForDepartment() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.manager.getUsername(), this.manager, "ROLE_DEPARTMENT_MANAGER");
        mockMvc.perform(MockMvcRequestBuilders.get("/api/absences")
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id").value(mockedAbsence.getId().toString()));
    }

    @Test
    @WithMockUser(roles = "BUILDING_MANAGER")
    public void testThatCreateAbsenceIsSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_OWN_ABSENCE")
    public void testThatCreateAbsenceReturnsHttp400IfValidationFails() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_OWN_ABSENCE")
    public void testThatCreateAbsenceReturnsHttp400IfStartDateIsAfterEndDate() throws Exception {
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
    public void testThatCreateAbsenceReturnsHttp404IfManagerWasNotFound() throws Exception {
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
    public void testThatCreateAbsenceReturnsHttp400IfIdsAreEqual() throws Exception {
        AbsenceCreateDTO dto = new AbsenceCreateDTO(this.user.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5),
                AbsenceType.ILLNESS,
                "Comment",
                this.user.getId());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/absences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_OWN_ABSENCE")
    public void testThatCreateAbsenceReturnsHttp403IfManagerRoleIsIncorrect() throws Exception {
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
    public void testThatCreateAbsenceReturnsHttp403IfDepartmentOfManagerIsIncorrect() throws Exception {
        Department dep = TestDataUtil.createDepartmentEntity(this.building);
        dep.setName("Another department");
        dep = departmentService.createDepartment(dep);
        Room newRoom = roomService.createRoom(TestDataUtil.createRoomEntity(dep));
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
    public void testThatCreateAbsenceReturnsHttp201AndCreatedAbsence() throws Exception {
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
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_ABSENCES")
    public void testThatPatchAbsenceReturnsHttp400IfValidationFails() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/absences/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString("{}")))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_ABSENCES")
    public void testThatPatchAbsenceReturnsHttp404IfAbsenceWasNotFound() throws Exception {
        AbsencePatchDTO dto = new AbsencePatchDTO(AbsenceStatus.APPROVED);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/absences/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_ABSENCES")
    public void testThatPatchAbsenceReturnsHttp200OkAndUpdatedAbsence() throws Exception {
        AbsencePatchDTO dto = new AbsencePatchDTO(AbsenceStatus.APPROVED);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/absences/" + this.mockedAbsence.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(this.mockedAbsence.getId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(this.mockedAbsence.getStatus().name()));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_OWN_ABSENCE")
    public void testThatDeleteAbsenceReturnsHttp404IfAbsenceDoesNotExist() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/absences/" + UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testThatDeleteAbsenceReturnsHttp403ForbiddenIfAbsenceDoesNotBelongToUser() throws Exception {
        Userx otherUser = TestDataUtil.createUserxEntity(null, null);
        otherUser.setUsername("u");
        otherUser.setPassword("passwd");
        otherUser = userService.createNewUser(otherUser);
        TestingAuthenticationToken auth = new TestingAuthenticationToken(otherUser.getUsername(), otherUser, "CAN_MANAGE_OWN_ABSENCE");
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/absences/" + this.mockedAbsence.getId())
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value("You are not allowed to delete this absence."));
    }

    @Test
    public void testThatDeleteAbsenceReturnsHttp204NoContentOnSuccessfulDeletion() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.user.getUsername(), this.user, "CAN_MANAGE_OWN_ABSENCE");
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/absences/" + this.mockedAbsence.getId())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_ABSENCES")
    public void testThatGetSpecificAbsenceReturnsHttp404IfAbsenceWasNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/absences/" + UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testThatGetSpecificAbsenceReturnsHttp403IfAssignedIDIsOther() throws Exception {
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
    public void testThatGetSpecificAbsenceReturnsHttp200Ok() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(this.manager.getUsername(), this.manager, "CAN_MANAGE_ABSENCES");
        mockMvc.perform(MockMvcRequestBuilders.get("/api/absences/" + this.mockedAbsence.getId())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(this.mockedAbsence.getId().toString()));
    }

}
