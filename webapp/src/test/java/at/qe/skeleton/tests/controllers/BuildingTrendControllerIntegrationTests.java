package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.model.BuildingTrend;
import at.qe.skeleton.model.Trend;
import at.qe.skeleton.repositories.BuildingTrendRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.util.UUID;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BuildingTrendControllerIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired BuildingTrendRepository buildingTrendRepository;

    private BuildingTrend trend;

    @BeforeEach
    void setUp() {
        this.trend = BuildingTrend.builder()
                .departmentId(UUID.randomUUID())
                .departmentName("Test")
                .trend(Trend.UP)
                .value(28.0)
                .date(LocalDate.now().minusDays(5))
                .build();
        buildingTrendRepository.save(trend);
        trend.setTrend(Trend.DOWN);
        trend.setValue(18.0);
        trend.setDate(LocalDate.now().minusDays(3));
        buildingTrendRepository.save(trend);
        trend.setDepartmentId(UUID.randomUUID());
        trend.setValue(50.0);
        trend.setDate(LocalDate.now());
        buildingTrendRepository.save(trend);
    }

    @Test
    void testThatBuildingTrendEndpointsAreSecured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/building-trend/"+UUID.randomUUID()+"?startDate="+LocalDate.now()+"&endDate="+LocalDate.now().plusDays(1)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CAN_VIEW_COMPANY_AGGR")
    void testThatBuildingTrendEndpointReturnsHttp400BadRequestIfStartingDateIsLaterThanEndDate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/building-trend/departments/"+UUID.randomUUID()+"?startDate="+LocalDate.now()+"&endDate="+LocalDate.now().minusDays(1)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CAN_VIEW_COMPANY_AGGR")
    void testThatBuildingTrendEndpointReturnsHttp200OkAndEmptyListOnSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/building-trend/departments/"+UUID.randomUUID()+"?startDate="+LocalDate.now()+"&endDate="+LocalDate.now().plusDays(1)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "CAN_VIEW_COMPANY_AGGR")
    void testThatBuildingTrendEndpointReturnsHttp200OkAndListOnSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/building-trend/departments/"+this.trend.getDepartmentId()+"?startDate="+LocalDate.now()+"&endDate="+LocalDate.now().plusDays(1)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").doesNotExist());
    }

}
