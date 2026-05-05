package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.UserRoleCreateDTO;
import at.qe.skeleton.dtos.UserRoleDTO;
import at.qe.skeleton.model.Permission;
import at.qe.skeleton.services.UserRoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
public class RoleControllerIntegrationTests {

    private UserRoleService userRoleService;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @Autowired
    public RoleControllerIntegrationTests(UserRoleService userRoleService,
                                          MockMvc mockMvc) {
        this.userRoleService = userRoleService;
        this.mockMvc = mockMvc;
        this.objectMapper = new ObjectMapper();
    }

    @Test
    public void testThatPermissionsEndpointIsSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/roles"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"CAN_MANAGE_USERS"})
    public void testThatGetAllPermissionsReturnListOfPredefinedRoles() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/roles"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[3].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[4].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[5].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[6].id").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = {"CAN_MANAGE_USERS"})
    public void testThatUpdatePermissionReturnsHttp404NotFoundIfRoleDoesNotExist() throws Exception {
        UserRoleCreateDTO dto = new UserRoleCreateDTO("Test", null);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/roles/"+ UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"CAN_MANAGE_USERS"})
    public void testThatUpdatePermissionReturnsHttp200CreatedAndUpdatedObject() throws Exception {
        UserRoleCreateDTO dto = new UserRoleCreateDTO(null, Set.of(Permission.CAN_VIEW_ALL_ROOMS));
        UUID id = userRoleService.getListOfPermissions().getFirst().getId();
        String res = mockMvc.perform(MockMvcRequestBuilders.patch("/api/roles/"+ id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("EMPLOYEE"))
                .andReturn().getResponse().getContentAsString();
        UserRoleDTO mappedResponse = objectMapper.readValue(res, UserRoleDTO.class);
        assertTrue(mappedResponse.permissions().contains(Permission.CAN_VIEW_ALL_ROOMS));
        assertEquals(1, mappedResponse.permissions().size());
    }

}
