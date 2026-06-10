package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.UserSettingsPatchDTO;
import at.qe.skeleton.dtos.UserxCreateDTO;
import at.qe.skeleton.dtos.UserxPatchDTO;
import at.qe.skeleton.mappers.UserxCreateMapper;
import at.qe.skeleton.model.DateFormat;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.services.UserRoleService;
import at.qe.skeleton.services.UserService;
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

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserxControllerIntegrationTests {

    @Autowired UserService userService;
    @Autowired UserRoleService userRoleService;
    @Autowired UserxCreateMapper userxCreateMapper;
    @Autowired UserxRepository userxRepository;
    @Autowired MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userxRepository.deleteAll();
    }

    @Test
    void testThatUsersEndpointIsSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_USERS")
    void testThatGetPageOfUsersReturnsHttp200OkAndPage() throws Exception {
        Userx user = userService.createNewUser(userxCreateMapper.mapFrom(TestDataUtil.createUserxCreateDTO(Set.of(userRoleService.getListOfPermissions().getFirst().getId()))));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id").value(user.getId().toString()));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_USERS")
    void testThatGetSpecificUserReturnsHttp404NotFoundIfDoesNotExist() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/" + UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_USERS")
    void testThatGetSpecificUserReturnsHttp200OkAndUser() throws Exception {
        Userx user = userService.createNewUser(userxCreateMapper.mapFrom(TestDataUtil.createUserxCreateDTO(Set.of(userRoleService.getListOfPermissions().getFirst().getId()))));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/" + user.getId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(user.getId().toString()));
    }


    @Test
    @WithMockUser(authorities = "CAN_MANAGE_OWN_ABSENCE")
    void testThatGetPageOfAbsencesOfAuthenticatedUserReturnsHttp200Ok() throws Exception {
        Userx user = userService.createNewUser(userxCreateMapper.mapFrom(TestDataUtil.createUserxCreateDTO(Set.of(userRoleService.getListOfPermissions().getFirst().getId()))));
        TestingAuthenticationToken auth = new TestingAuthenticationToken(user, null, "CAN_MANAGE_OWN_ABSENCE");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/me/absences")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_USERS")
    void testThatCreateNewUserReturnsHttp201Created() throws Exception {
        UserxCreateDTO dto = TestDataUtil.createUserxCreateDTO(Set.of(userRoleService.getListOfPermissions().getFirst().getId()));
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(dto.username()));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_USERS")
    void testThatCreateNewUserReturnsHttp400BadRequestIfInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_USERS")
    void testThatPatchSpecificUserReturnsHttp200Ok() throws Exception {
        Userx user = userService.createNewUser(userxCreateMapper.mapFrom(TestDataUtil.createUserxCreateDTO(Set.of(userRoleService.getListOfPermissions().getFirst().getId()))));

        UserxPatchDTO patchDTO = new UserxPatchDTO("updatedUser", "Updated", "Name", false, Collections.emptySet(), null);
        String json = objectMapper.writeValueAsString(patchDTO);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value("updatedUser"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_USERS")
    void testThatPatchSpecificUserReturnsHttp409Conflict() throws Exception {
        Userx user = userService.createNewUser(userxCreateMapper.mapFrom(TestDataUtil.createUserxCreateDTO(Set.of(userRoleService.getListOfPermissions().getFirst().getId()))));
        Userx user2 = userxCreateMapper.mapFrom(TestDataUtil.createUserxCreateDTO(Set.of(userRoleService.getListOfPermissions().getFirst().getId())));
        user2.setUsername("updatedUser");
        userService.createNewUser(user2);

        UserxPatchDTO patchDTO = new UserxPatchDTO("updatedUser", "Updated", "Name", false, Collections.emptySet(), null);
        String json = objectMapper.writeValueAsString(patchDTO);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_USERS")
    void testThatPatchSpecificUserReturnsHttp404NotFoundIfDoesNotExist() throws Exception {
        UserxPatchDTO patchDTO = new UserxPatchDTO("updatedUser", "Updated", "Name", false, Collections.emptySet(), null);
        String json = objectMapper.writeValueAsString(patchDTO);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/users/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }


    @Test
    @WithMockUser(authorities = "CAN_MANAGE_USERS")
    void testThatDeleteAnUserReturnsHttp204NoContentAndDeletesUser() throws Exception {
        Userx user = userService.createNewUser(userxCreateMapper.mapFrom(TestDataUtil.createUserxCreateDTO(Set.of(userRoleService.getListOfPermissions().getFirst().getId()))));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/users/" + user.getId()))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(0));
    }

    @Test
    void testThatGetAuthenticatedUserReturnsHttp200OkAndAuthenticatedUser() throws Exception {
        Userx user = userService.createNewUser(userxCreateMapper.mapFrom(TestDataUtil.createUserxCreateDTO(Set.of(userRoleService.getListOfPermissions().getFirst().getId()))));
        TestingAuthenticationToken token = new TestingAuthenticationToken(user.getUsername(), user, "ROLE_EMPLOYEE");
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/me")
                .with(SecurityMockMvcRequestPostProcessors.authentication(token)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(user.getUsername()));
    }

    @Test
    void testThatGetUserSettingsReturnsHttp200Ok() throws Exception {
        Userx user = userService.createNewUser(userxCreateMapper.mapFrom(TestDataUtil.createUserxCreateDTO(Set.of(userRoleService.getListOfPermissions().getFirst().getId()))));
        TestingAuthenticationToken token = new TestingAuthenticationToken(user.getUsername(), user, "ROLE_EMPLOYEE");
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/settings")
                .with(SecurityMockMvcRequestPostProcessors.authentication(token)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.darkMode").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.fahrenheit").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.twelveHourFormat").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.format").value(DateFormat.DD_MM_YYYY.name()));
    }

    @Test
    void testThatPatchUserSettingsReturnsHttp200Ok() throws Exception {
        Userx user = userService.createNewUser(userxCreateMapper.mapFrom(TestDataUtil.createUserxCreateDTO(Set.of(userRoleService.getListOfPermissions().getFirst().getId()))));
        TestingAuthenticationToken token = new TestingAuthenticationToken(user.getUsername(), user, "ROLE_EMPLOYEE");
        UserSettingsPatchDTO dto =
                new UserSettingsPatchDTO(true, null, null, true, null, null, null);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/users/settings")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.darkMode").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.fahrenheit").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.twelveHourFormat").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.format").value(DateFormat.DD_MM_YYYY.name()));
    }
}