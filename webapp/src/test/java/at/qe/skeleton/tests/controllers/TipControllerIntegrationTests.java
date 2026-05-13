package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.TipCreateDTO;
import at.qe.skeleton.dtos.TipPatchDTO;
import at.qe.skeleton.model.Tip;
import at.qe.skeleton.model.ViolatedSensor;
import at.qe.skeleton.model.ViolationType;
import at.qe.skeleton.model.WarningStatus;
import at.qe.skeleton.repositories.TipRepository;
import at.qe.skeleton.services.TipService;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TipControllerIntegrationTests {

    @Autowired
    TipService tipService;
    @Autowired
    TipRepository tipRepository;
    @Autowired
    MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        tipRepository.deleteAll();
    }

    @Test
    void testThatTipsEndpointsAreSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/tips"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_TIPS")
    void testThatCreateTipReturnsHttp400BadRequestIfValidationFails() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_TIPS")
    void testThatCreateTipReturnsHttp409ConflictIfSuchTypeOfTipAlreadyExists() throws Exception {
        tipService.createTip(Tip.builder().violationType(ViolationType.UNDER)
                .violatedSensor(ViolatedSensor.TEMPERATURE)
                .violationStatus(WarningStatus.GREEN)
                .msg("MSG").build());
        TipCreateDTO dto = new TipCreateDTO(ViolationType.UNDER, ViolatedSensor.TEMPERATURE, WarningStatus.GREEN, "Another msg");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_TIPS")
    void testThatCreateTipReturnsHttp201CreatedIfSuccessful() throws Exception {
        TipCreateDTO dto = new TipCreateDTO(ViolationType.UNDER, ViolatedSensor.TEMPERATURE, WarningStatus.GREEN, "Another msg");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_TIPS")
    void testThatGetAllTipsReturnsHttp200OkAndListOfTips() throws Exception {
        tipService.createTip(Tip.builder()
                .violatedSensor(ViolatedSensor.TEMPERATURE)
                .violationStatus(WarningStatus.GREEN)
                .violationType(ViolationType.UNDER)
                .msg("MSG").build());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/tips"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].message").value("MSG"))
                .andExpect(MockMvcResultMatchers.jsonPath("[0].id").exists());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_TIPS")
    void testThatDeleteSpecificTipReturnsHttp204NoContentIfTipIsDeleted() throws Exception {
        Tip tip = tipService.createTip(Tip.builder()
                .violatedSensor(ViolatedSensor.TEMPERATURE)
                .violationStatus(WarningStatus.GREEN)
                .violationType(ViolationType.UNDER)
                .msg("MSG").build());
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/tips/"+tip.getId()))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        assertEquals(0, tipService.getAllTips().size());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_TIPS")
    void testThatPatchMessageForTipReturnsHttp400BadRequestIfValidationFails() throws Exception {
        TipPatchDTO dto = new TipPatchDTO("");
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/tips/"+UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_TIPS")
    void testThatPatchMessageForTipReturnsHttp404NotFoundIfTipWasNotFound() throws Exception {
        TipPatchDTO dto = new TipPatchDTO("new tip");
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/tips/"+UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_TIPS")
    void testThatPatchMessageForTipReturnsHttp200OkIfMessageUpdatedSuccessfully() throws Exception {
        Tip tip = tipService.createTip(Tip.builder()
                .violatedSensor(ViolatedSensor.TEMPERATURE)
                .violationStatus(WarningStatus.GREEN)
                .violationType(ViolationType.UNDER)
                .msg("MSG").build());
        TipPatchDTO dto = new TipPatchDTO("new tip");
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/tips/"+tip.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(dto.message()));
    }

    @Test
    @WithMockUser(authorities = "CAN_MANAGE_TIPS")
    void testThatDeleteSpecificTipReturnsHttp204NoContentEvenIfTipDoesNotExist() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/tips/"+ UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        assertEquals(0, tipService.getAllTips().size());
    }

}
