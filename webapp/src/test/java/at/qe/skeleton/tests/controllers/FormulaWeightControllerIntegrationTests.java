package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.FormulaWeightCreateDTO;
import at.qe.skeleton.model.FormulaWeights;
import at.qe.skeleton.repositories.FormulaWeightsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FormulaWeightControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FormulaWeightsRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLOYEE")
    void testThatFormulaWeightsEndpointsAreSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/weights"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/weights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_WEIGHT_FORMULA")
    void testThatGetFormulaWeightReturnsDefaultWeightsIfNotSet() throws Exception {
        assertTrue(repository.findAll().isEmpty());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/weights"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.tempWeight").value(0.4))
                .andExpect(MockMvcResultMatchers.jsonPath("$.humWeight").value(0.3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.co2Weight").value(0.3));
        assertFalse(repository.findAll().isEmpty());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_WEIGHT_FORMULA")
    void testThatGetFormulaWeightReturnsSpecificWeightsIfSet() throws Exception {
        repository.save(FormulaWeights.builder().co2Weight(0.5).tempWeight(0.3).humWeight(0.2).modifiedAt(LocalDateTime.now()).build());
        assertFalse(repository.findAll().isEmpty());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/weights"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.tempWeight").value(0.3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.humWeight").value(0.2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.co2Weight").value(0.5));
        assertFalse(repository.findAll().isEmpty());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_WEIGHT_FORMULA")
    void testThatGetFormulaWeightReturnsHttp400BadRequestIfValidationFails() throws Exception {
        // EMPTY VALUES
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/weights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        assertTrue(repository.findAll().isEmpty());
        // OVERFLOW
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/weights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new FormulaWeightCreateDTO(0.8, 0.2, 0.3))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        assertTrue(repository.findAll().isEmpty());
        // UNDERFLOW
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/weights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new FormulaWeightCreateDTO(0.1, 0.2, 0.3))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_WEIGHT_FORMULA")
    void testThatPatchFormulaWeightReturnsNewWeightsIfNotPreviouslySetWithDTOsValues() throws Exception {
        assertTrue(repository.findAll().isEmpty());
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/weights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new FormulaWeightCreateDTO(0.5, 0.2, 0.3))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.tempWeight").value(0.5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.humWeight").value(0.3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.co2Weight").value(0.2));
        assertFalse(repository.findAll().isEmpty());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_WEIGHT_FORMULA")
    void testThatPatchFormulaWeightUpdatesNewWeights() throws Exception {
        repository.save(FormulaWeights.builder().co2Weight(0.5).tempWeight(0.5).humWeight(0.0).modifiedAt(LocalDateTime.now()).build());
        assertFalse(repository.findAll().isEmpty());
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/weights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new FormulaWeightCreateDTO(0.5, 0.2, 0.3))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.tempWeight").value(0.5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.humWeight").value(0.3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.co2Weight").value(0.2));
        assertFalse(repository.findAll().isEmpty());
    }
}
