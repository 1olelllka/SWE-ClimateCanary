package at.qe.skeleton.tests;

import at.qe.skeleton.configs.JwtConfig;
import at.qe.skeleton.configs.JwtTokenProvider;
import at.qe.skeleton.configs.TokenAuthenticationFilter;
import at.qe.skeleton.controllers.AdminController;
import at.qe.skeleton.dtos.UserxCreateDTO;
import at.qe.skeleton.dtos.UserxDTO;
import at.qe.skeleton.mappers.UserxCreateMapper;
import at.qe.skeleton.mappers.UserxMapper;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.model.UserxRole;
import at.qe.skeleton.services.UserxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Some very basic tests for {@link AdminController}.
 *
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by the University of Innsbruck.
 */
@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoSpyBean
    private TokenAuthenticationFilter tokenAuthenticationFilter;

    @MockitoBean
    private JwtConfig jwtConfig;

    @MockitoBean
    private UserxService userService;

    @MockitoBean
    private UserxMapper userMapper;

    @MockitoBean
    private UserxCreateMapper userCreateMapper;

    @BeforeEach
    void setUp() throws Exception {
        // mock setup of the Authentication Filter
        Mockito.doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(tokenAuthenticationFilter).doFilterInternal(
                Mockito.any(HttpServletRequest.class),
                Mockito.any(HttpServletResponse.class),
                Mockito.any(FilterChain.class)
        );

        @SuppressWarnings("unchecked")
        Jws<Claims> mockJws = (Jws<Claims>) Mockito.mock(Jws.class);
        Claims mockClaims = Mockito.mock(Claims.class);
        Mockito.when(mockClaims.getSubject()).thenReturn("admin");
        Mockito.when(mockJws.getPayload()).thenReturn(mockClaims);
        Mockito.when(jwtTokenProvider.validateTokenAndGetJws(Mockito.anyString()))
                .thenReturn(Optional.of(mockJws));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    void getAllUsers() throws Exception {
        Long id = 1L;
        String username = "testUser";
        Userx user1 = new Userx();
        user1.setId(id);
        user1.setUsername(username);
        user1.setFirstName("First");
        user1.setLastName("Last");
        List<Userx> users = List.of(user1);

        Mockito.when(userService.getAllUsers()).thenReturn(users);
        Mockito.when(userMapper.mapTo(Mockito.any(Userx.class))).thenReturn(new UserxDTO(
                id, null, null, null, null, "testUser", "First", "Last", null, null, false, null));


        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(id))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].username").value(username));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    void getUserUserExists() throws Exception {
        Long id = 1L;
        String username = "testUser";
        Userx user1 = new Userx();
        user1.setId(id);
        user1.setUsername(username);
        user1.setFirstName("First");
        user1.setLastName("Last");
        Mockito.when(userService.loadUser(id)).thenReturn(Optional.of(user1));
        Mockito.when(userMapper.mapTo(user1)).thenReturn(new UserxDTO(id, null, null, null, null, username, "First", "Last", null, null, false, null));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/{id}", id))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(username));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    void getUserUserDoesNotExist() throws Exception {
        Mockito.when(userService.loadUser(1L)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/1"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    void createUserValidInput() throws Exception {
        Long id = 1L;
        String username = "newUser";
        String password = "password";
        String firstName = "first";
        String lastName = "last";
        String email = "new@example.com";
        Set<UserxRole> roles = Set.of(UserxRole.ADMIN);
        boolean isEnabled = true;

        UserxCreateDTO newUser = new UserxCreateDTO(username, password, firstName, lastName, email, "", true, roles);
        Userx user = new Userx();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setEnabled(isEnabled);

        Mockito.when(userCreateMapper.mapFrom(newUser)).thenReturn(user);
        Mockito.when(userService.saveUser(user)).thenReturn(user);
        Mockito.when(userMapper.mapTo(user)).thenReturn(new UserxDTO(id, null, null, null, null, username, firstName, lastName, email, "", isEnabled, roles));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newUser)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value(username));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    void deleteUserUserExists() throws Exception {
        Long id = 1L;
        String username = "newUser";
        Userx user = new Userx();
        user.setId(id);
        user.setUsername(username);

        Mockito.when(userService.loadUser(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/{id}", id)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    void deleteUserUserDoesNotExist() throws Exception {
        Long id = 1L;
        Mockito.when(userService.loadUser(id)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/{id}", id)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

}
