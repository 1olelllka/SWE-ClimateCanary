package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.LoginRequestDTO;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.services.UserRoleService;
import at.qe.skeleton.tests.TestDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthenticationControllerIntegrationTests {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserxRepository userxRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserRoleService roleService;

    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userxRepository.deleteAll();
    }

    @Test
    void testThatLoginExistingUserReturnsNewJWTToken() throws Exception {
        Userx user = TestDataUtil.createUserxEntity(roleService.getListOfPermissions().getFirst(), null);
        user.setPassword(passwordEncoder.encode("passwd"));
        userxRepository.save(user);
        LoginRequestDTO dto = new LoginRequestDTO(user.getUsername(), "passwd");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.bearerToken").exists());
    }

    @Test
    void testThatLoginReturnsHttp400IfValidationFails() throws Exception {
        Userx user = TestDataUtil.createUserxEntity(roleService.getListOfPermissions().getFirst(), null);
        user.setPassword(passwordEncoder.encode("passwd"));
        userxRepository.save(user);
        LoginRequestDTO dto = new LoginRequestDTO("", "passwd");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void testThatLoginReturnsHttp401IfUsernameOrPasswordIsIncorrect() throws Exception {
        Userx user = TestDataUtil.createUserxEntity(roleService.getListOfPermissions().getFirst(), null);
        user.setPassword(passwordEncoder.encode("passwd"));
        userxRepository.save(user);
        LoginRequestDTO dto = new LoginRequestDTO("incorrect_user", "passwd");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

}
